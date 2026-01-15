package com.stellantis.lwm2m.mcp.client.cot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellantis.lwm2m.mcp.client.cot.agent.SqlValidatorAgent;
import com.stellantis.lwm2m.mcp.client.cot.service.CoTDecisionService;
import com.stellantis.lwm2m.mcp.client.service.DatabaseService;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RequestMapping("/api/v3/cot-chat")
@RestController
public class CoTChatStreamController {
  private final SimpleLoggerAdvisor advisor = new SimpleLoggerAdvisor();

  private static final Logger log = LoggerFactory.getLogger(CoTChatStreamController.class);
  private static final int DEFAULT_TOP_K = 5;
  private static final int MAX_LOG_SQL_LEN = 20000;
  private static final int LAST_N_MESSAGES = 5;

  private final ChatClient chatClient;
  private final ChatMemory chatMemory;
  private final SyncMcpToolCallbackProvider syncMcpToolCallbackProvider;
  private final DatabaseService databaseService;

  private final IntentEntityExtractor extractor;
  private final CoTDecisionService decisionService;
  private final ObjectMapper objectMapper;
  private final SqlValidatorAgent sqlValidator;
  private final ChatMemoryRepository chatMemoryRepository;

  public CoTChatStreamController(
      ChatClient.Builder chatClientBuilder,
      ChatMemoryRepository chatMemoryRepository,
      @Qualifier("lwm2m-mcp-server-callback-tool-provider")
          SyncMcpToolCallbackProvider syncMcpToolCallbackProvider,
      DatabaseService databaseService,
      IntentEntityExtractor extractor,
      CoTDecisionService decisionService,
      ObjectMapper objectMapper,
      SqlValidatorAgent sqlValidator) {

    this.syncMcpToolCallbackProvider = syncMcpToolCallbackProvider;
    this.databaseService = databaseService;
    this.extractor = extractor;
    this.decisionService = decisionService;
    this.objectMapper = objectMapper;
    this.sqlValidator = sqlValidator;
    this.chatMemoryRepository = chatMemoryRepository;

    this.chatMemory =
        MessageWindowChatMemory.builder()
            .maxMessages(10)
            .chatMemoryRepository(chatMemoryRepository)
            .build();

    this.chatClient =
        chatClientBuilder
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
            .build();
  }

  @PostMapping(path = "/stream", produces = "application/octet-stream")
  public ResponseEntity<Flux<String>> streamChat(
      @RequestParam(required = false) UUID conversationId, @RequestBody Map<String, String> body) {

    String userQuery = body.get("text");
    String conversationIdStr = body.get("conversationId");

    if (userQuery == null || userQuery.isBlank()) {
      return ResponseEntity.badRequest()
          .body(Flux.just(formatSseData("error", Map.of("message", "Empty query"))));
    }

    String cleanQuery = userQuery.trim();
    boolean isDbCommand = cleanQuery.toLowerCase().startsWith("/db");
    final String traceId = UUID.randomUUID().toString();
    final long t0 = System.nanoTime();

    UUID convId = null;

    if (conversationIdStr != null && !conversationIdStr.isBlank()) {
      try {
        convId = UUID.fromString(conversationIdStr);
      } catch (IllegalArgumentException e) {
        // Invalid UUID format - create new one
        convId = UUID.randomUUID();
      }
    } else {
      // No conversationId provided - create new one
      convId = UUID.randomUUID();
    }

    log.info("traceId={} step=START conversationId={} userQuery='{}'", traceId, convId, cleanQuery);

    try {

      if (isDbCommand) {
        cleanQuery = cleanQuery.replaceFirst("(?i)^/db\\s*", "");
        return handleDbMode(cleanQuery, convId, traceId, t0);
      }

      UUID finalConvId = convId;
      Flux<String> responseFlux =
          chatClient
              .prompt()
              .user(cleanQuery)
              .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConvId.toString()))
              .stream()
              .content()
              .map(chunk -> formatSseData("response_chunk", Map.of("chunk", chunk)))
              .concatWith(
                  Flux.just(
                      formatSseData(
                          "complete",
                          Map.of(
                              "conversationId",
                              convId,
                              "totalTimeMs",
                              toMs(System.nanoTime() - t0)))))
              .onErrorResume(
                  e ->
                      Flux.just(
                          formatSseData(
                              "error", Map.of("message", e.getMessage(), "traceId", traceId))));

      return ResponseEntity.ok()
          .header("Cache-Control", "no-cache")
          .header("Connection", "keep-alive")
          .header("X-Accel-Buffering", "no")
          .body(responseFlux);

    } catch (Exception e) {
      log.error("traceId={} step=ERROR msg='{}'", traceId, e.getMessage(), e);
      Flux<String> errorFlux =
          Flux.just(formatSseData("error", Map.of("message", e.getMessage(), "traceId", traceId)));
      return ResponseEntity.ok(errorFlux);
    }
  }

  private ResponseEntity<Flux<String>> handleDbMode(
      String query, UUID convId, String traceId, long t0) {
    log.info("traceId={} step=DB_MODE userQuery='{}'", traceId, query);

    // ---- Step 1: Context loading and classification
    List<Message> history = chatMemoryRepository.findByConversationId(convId.toString());
    QueryAnalysis analysis;

    if (!history.isEmpty()) {
      analysis = extractor.analyze(query, history); // contextual mode
    } else {
      analysis = extractor.analyze(query); // fresh intent mode
    }

    log.debug("traceId={} analysis={}", traceId, analysis);

    if (analysis.isFollowUp()) {
      log.info("traceId={} step=FOLLOW_UP detected, skipping analysis and validation.", traceId);

      chatMemory.add(convId.toString(), new UserMessage(query));

      Flux<String> followUpResponseFlux =
          Flux.concat(
                  Flux.just(
                      formatSseData("sql_generated", Map.of("sqlQuery", analysis.finalSql()))),
                  chatClient
                      .prompt()
                      .user(analysis.finalSql())
                      .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId.toString()))
                      .toolCallbacks(syncMcpToolCallbackProvider)
                      .stream()
                      .content()
                      .map(chunk -> formatSseData("response_chunk", Map.of("chunk", chunk))),
                  Flux.just(
                      formatSseData(
                          "complete",
                          Map.of(
                              "conversationId",
                              convId,
                              "sqlQuery",
                              analysis.finalSql(),
                              "totalTimeMs",
                              toMs(System.nanoTime() - t0)))))
              .delayElements(Duration.ofMillis(20));

      return ResponseEntity.ok()
          .header("Cache-Control", "no-cache")
          .header("Connection", "keep-alive")
          .header("X-Accel-Buffering", "no")
          .body(followUpResponseFlux);
    }

    var decision = decisionService.decide(query, DEFAULT_TOP_K, convId);

    String sqlQuery = databaseService.generateCotSqlWithLlm(query, decision);
    log.info("traceId={} step=SQL sqlPreview='{}'", traceId, truncate(sqlQuery, MAX_LOG_SQL_LEN));

    // SQL Validation (reasoning step)
    ValidationResult validation =
        sqlValidator.validate(query, sqlQuery, decision.fullSchemaContext());

    if (!validation.isValid()) {
      // SSE event back to user
      return ResponseEntity.ok(
          Flux.just(
              formatSseData(
                  "sql_validation_failed",
                  Map.of(
                      "sqlQuery", sqlQuery,
                      "issues", validation.getIssues(),
                      "suggestion", validation.getSuggestion()))));
    }

    chatMemory.add(convId.toString(), new UserMessage(query));

    StringBuilder assistantResponse = new StringBuilder();

    Flux<String> responseFlux =
        Flux.concat(
                Flux.just(
                    formatSseData(
                        "analysis", Map.of("intent", "analysis.intent()", "entities", ""))),
                Flux.just(
                    formatSseData(
                        "schema_selected",
                        Map.of("selectedSchema", decision.selectedSchemaTable()))),
                Flux.just(formatSseData("sql_generated", Map.of("sqlQuery", sqlQuery))),
                chatClient
                    .prompt()
                    .user(sqlQuery)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId.toString()))
                    .toolCallbacks(syncMcpToolCallbackProvider)
                    .stream()
                    .content()
                    .doOnNext(assistantResponse::append)
                    .doOnComplete(
                        () -> {
                          try {
                            Map<String, Object> meta = new LinkedHashMap<>();
                            meta.put("sqlQuery", sqlQuery);
                            if (decision != null && decision.selectedSchemaTable() != null) {
                              meta.put("schema", decision.selectedSchemaTable());
                              meta.put("table", decision.selectedSchemaTable());
                            }
                            if (analysis != null) {
                              meta.put("intent", analysis.intent());
                              meta.put("entities", analysis.entities());
                              meta.put("filters", analysis.filters());
                            }

                            String messageJson =
                                objectMapper.writeValueAsString(
                                    Map.of(
                                        "role",
                                        "ASSISTANT",
                                        "content",
                                        assistantResponse.toString(),
                                        "meta",
                                        meta));

                            chatMemory.add(convId.toString(), new AssistantMessage(messageJson));

                            log.info(
                                "traceId={} step=MEMORY_STORED metaKeys={} length={}",
                                traceId,
                                meta.keySet(),
                                assistantResponse.length());
                          } catch (Exception e) {
                            log.error(
                                "traceId={} step=MEMORY_STORE_ERROR msg='{}'",
                                traceId,
                                e.getMessage(),
                                e);
                          }
                        })
                    .map(chunk -> formatSseData("response_chunk", Map.of("chunk", chunk)))
                    .onErrorResume(
                        e -> Flux.just(formatSseData("error", Map.of("message", e.getMessage())))),
                Flux.just(
                    formatSseData(
                        "complete",
                        Map.of(
                            "conversationId", convId,
                            "sqlQuery", sqlQuery,
                            "totalTimeMs", toMs(System.nanoTime() - t0)))))
            .delayElements(Duration.ofMillis(20));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-cache")
        .header("Connection", "keep-alive")
        .header("X-Accel-Buffering", "no")
        .body(responseFlux);
  }

  private String formatSseData(String eventType, Object data) {
    try {
      String jsonData =
          objectMapper.writeValueAsString(
              Map.of("type", eventType, "data", data, "timestamp", System.currentTimeMillis()));
      return "data: " + jsonData + "\n\n";
    } catch (Exception e) {
      return "data: {\"type\":\"error\",\"data\":{\"message\":\"Serialization error\"}}\n\n";
    }
  }

  private static long toMs(long nanos) {
    return nanos / 1_000_000L;
  }

  private static String truncate(String s, int max) {
    return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
  }
}
