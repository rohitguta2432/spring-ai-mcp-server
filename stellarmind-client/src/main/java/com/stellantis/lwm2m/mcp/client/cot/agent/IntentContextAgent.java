package com.stellantis.lwm2m.mcp.client.cot.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellantis.lwm2m.mcp.client.cot.QueryAnalysis;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

/**
 * {@code IntentContextAgent} is an LLM-powered reasoning component responsible for understanding
 * whether a user's latest natural-language query is a follow-up to a previous conversational
 * context or a new standalone intent.
 *
 * <p>It analyzes the most recent {@link Message} history (typically last 3–5 messages) and uses the
 * provided {@link ChatClient} to perform reasoning based on both user and assistant turns.
 *
 * <p>This agent centralizes all conversational context-awareness logic used during Text-to-SQL
 * (T2SQL) query generation, ensuring that schema/table continuity and intent refinement across
 * turns are preserved.
 *
 * <p><b>Typical Scenario:</b>
 *
 * <pre>
 *   USER:  "List pending DM operations"
 *   ASSISTANT:  (executes query on gtw.device_operation_reference WHERE status='QUEUED')
 *
 *   USER:  "Now show failed ones"
 *   → IntentContextAgent infers follow-up = true,
 *     schema/table reused = gtw.device_operation_reference,
 *     finalSql = SELECT ... WHERE status='FAILED'
 * </pre>
 */
@Service
public class IntentContextAgent {

  private static final Logger log = LoggerFactory.getLogger(IntentContextAgent.class);

  /** LLM chat client used for reasoning. */
  private final ChatClient chatClient;

  /** Shared JSON mapper for serialization and parsing. */
  private final ObjectMapper objectMapper;

  public IntentContextAgent(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  /**
   * Determines whether the user's latest query continues from the previous conversational context
   * or starts a new one.
   *
   * <p>The method inspects the recent conversation history and constructs a condensed textual
   * representation of prior turns, which is then sent to the LLM for reasoning. The LLM is asked to
   * respond strictly in JSON that maps to {@link QueryAnalysis}.
   *
   * @param userQuery the user's latest natural-language query
   * @param history ordered list of prior chat messages (oldest → newest)
   * @return parsed {@link QueryAnalysis} indicating continuity and reasoning
   */
  public QueryAnalysis analyze(String userQuery, List<Message> history) {
    try {
      // ---- Step 1: Condense the most recent conversation context
      StringBuilder contextBuilder = new StringBuilder();
      int start = Math.max(0, history.size() - 5); // use last 5 turns

      for (int i = start; i < history.size(); i++) {
        Message m = history.get(i);
        String content = m.getText();

        // Normalize potential JSON-formatted assistant messages containing meta info
        try {
          if (content != null && content.trim().startsWith("{") && content.contains("\"meta\"")) {
            JsonNode node = objectMapper.readTree(content);

            if (node.has("content")) {
              content = node.get("content").asText();
            }

            if (node.has("meta")) {
              JsonNode meta = node.get("meta");
              if (meta.has("sqlQuery")) {
                content += " [sql=" + meta.get("sqlQuery").asText() + "]";
              }
              if (meta.has("filters")) {
                content += " [filters=" + meta.get("filters").toString() + "]";
              }
            }
          }
        } catch (Exception ex) {
          log.debug(
              "Context normalization skipped for non-JSON message. messageType={} reason={}",
              m.getMessageType(),
              ex.getMessage());
        }

        contextBuilder
            .append(m.getMessageType())
            .append(": ")
            .append(content)
            .append(System.lineSeparator());
      }

      // ---- Step 2: Construct the reasoning prompt for the LLM
      String prompt =
          """
                    You are a Context-Aware SQL Reasoning Agent.

                    TASK:
                    Determine whether the user's latest message continues the previous SQL context
                    or starts a new intent. Use the conversation context below for reference.

                    RULES:
                    - If the query refines, filters, or expands the previous result → isFollowUp = true.
                    - If it introduces new entities, tables, or unrelated concepts → isFollowUp = false.
                    - For follow-ups, reuse the previous schema/table and suggest updated SQL if possible.

                    Respond ONLY in JSON with this structure:
                    {
                      "intent": "SELECT",
                      "entities": ["entity1"],
                      "filters": {"status":"failed"},
                      "error": "",
                      "isFollowUp": true/false,
                      "schema": "string or null",
                      "table": "string or null",
                      "finalSql": "string or null",
                      "reasoning": "short explanation"
                    }

                    CONVERSATION CONTEXT (latest last):
                    %s

                    USER QUERY:
                    %s
                    """
              .formatted(contextBuilder, userQuery);

      log.debug("🧠 [IntentContextAgent] Constructed prompt:\n{}", prompt);

      // ---- Step 3: Invoke the LLM
      String response = chatClient.prompt().user(prompt).call().content();

      log.debug("🧠 [IntentContextAgent] Raw LLM response: {}", response);

      // ---- Step 4: Parse structured JSON output directly into QueryAnalysis
      QueryAnalysis result = objectMapper.readValue(response, QueryAnalysis.class);

      log.info(
          "✅ [IntentContextAgent] Decision: followUp={}, schema={}, table={}, reasoning={}",
          result.isFollowUp(),
          result.schema(),
          result.table(),
          result.reasoning());

      return result;

    } catch (Exception e) {
      log.error("❌ [IntentContextAgent] Failure for query='{}' → {}", userQuery, e.getMessage(), e);

      // Safe fallback
      return new QueryAnalysis(
          "SELECT",
          List.of(),
          Map.of(),
          e.getMessage(),
          false,
          null,
          null,
          null,
          "LLM reasoning failure: " + e.getMessage());
    }
  }
}
