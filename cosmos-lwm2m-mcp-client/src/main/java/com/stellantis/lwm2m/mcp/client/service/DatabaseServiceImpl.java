package com.stellantis.lwm2m.mcp.client.service;

import com.stellantis.lwm2m.mcp.client.cot.CoTDecisionResult;
import com.stellantis.lwm2m.mcp.client.execption.ReadOnlyViolationException;
import com.stellantis.lwm2m.mcp.client.execption.SqlGenerationException;
import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunk;
import com.stellantis.lwm2m.mcp.client.repository.KnowledgeChunkJsonRepository;
import com.stellantis.lwm2m.mcp.client.repository.KnowledgeChunkRepository;
import com.stellantis.lwm2m.mcp.client.service.embeddings.HybridEmbeddingModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link DatabaseService} that:
 *
 * <ul>
 *   <li>Retrieves pgvector-backed schema context from {@code gtw.knowledge_chunks} using a
 *       repository.
 *   <li>Generates SQL via a configured {@link ChatClient} (LLM model is picked from
 *       application.yml).
 *   <li>Enforces read-only SQL (SELECT/WITH only) before execution.
 *   <li>Executes SQL using Spring {@link JdbcTemplate}.
 * </ul>
 */
@Service
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  /** Default K if a caller passes <= 0. */
  private static final int DEFAULT_TOP_K = 10;

  /** Max context characters to avoid oversized prompts. */
  private static final int MAX_CONTEXT_LENGTH = 20_000;

  private final HybridEmbeddingModel embeddingService; // model picked from yml; no hardcoding
  private final ChatClient.Builder chatClientBuilder; // model picked from yml; no hardcoding
  private final KnowledgeChunkRepository repo;
  private final KnowledgeChunkJsonRepository knowledgeChunkJsonRepository;
  private final JdbcTemplate jdbc;

  @Autowired
  public DatabaseServiceImpl(
      HybridEmbeddingModel embeddingService,
      @Lazy ChatClient.Builder chatClientBuilder,
      KnowledgeChunkRepository repo,
      JdbcTemplate jdbc,
      KnowledgeChunkJsonRepository knowledgeChunkJsonRepository) {
    this.embeddingService = embeddingService;
    this.chatClientBuilder = chatClientBuilder;
    this.repo = repo;
    this.jdbc = jdbc;
    this.knowledgeChunkJsonRepository = knowledgeChunkJsonRepository;
  }

  /** Executes a raw SQL query and returns rows as key-value maps. */
  @Override
  public List<Map<String, Object>> queryForList(String sql) {
    if (!StringUtils.hasText(sql)) {
      throw new SqlGenerationException("SQL must not be empty");
    }
    return jdbc.queryForList(sql);
  }

  /**
   * Performs a semantic search over {@code gtw.knowledge_chunks} using pgvector.
   *
   * <p>Returns the chunk contents (ordered by similarity) to keep the DatabaseService interface
   * decoupled from entity details.
   *
   * @param userPrompt user natural language request
   * @param topK number of context chunks desired (falls back to DEFAULT_TOP_K if &le; 0)
   * @return ordered list of chunk contents
   */
  @Override
  public List<String> findRelevantSchemaContext(String userPrompt, int topK) {
    if (!StringUtils.hasText(userPrompt)) {
      throw new IllegalArgumentException("userPrompt must not be empty");
    }
    int k = topK > 0 ? topK : DEFAULT_TOP_K;

    // 1) Embed the prompt using configured embedding model (no hardcoded model name).
    float[] rawVector = embeddingService.embed(userPrompt);
    if (rawVector == null || rawVector.length == 0) {
      throw new SqlGenerationException("Empty embedding vector from embedding model");
    }

    // 2) Convert to pgvector literal: [v1,v2,...,vn]
    String pgVectorLiteral = toPgVectorLiteral(rawVector);

    // 3) Similarity search via repository (uses <-> operator).
    List<KnowledgeChunk> chunks = repo.findMostRelevant(pgVectorLiteral, k);
    if (chunks == null || chunks.isEmpty()) {
      throw new SqlGenerationException("No relevant schema information found for the prompt");
    }

    // 4) Return contents only (interface requires List<String>)
    List<String> contents = new ArrayList<>(chunks.size());
    for (KnowledgeChunk c : chunks) {
      contents.add(c.getContent());
    }
    return contents;
  }

  /**
   * Calls the LLM (via {@link ChatClient}) to produce a single read-only SQL query.
   *
   * <ul>
   *   <li>Returns only SQL text (no markdown fences, no trailing semicolon).
   *   <li>Rejects queries that are not strictly read-only (SELECT / WITH...SELECT).
   * </ul>
   */
  @Override
  public String generateSqlWithLlm(String userPrompt, List<String> schemaContext) {
    if (!StringUtils.hasText(userPrompt)) {
      throw new IllegalArgumentException("userPrompt must not be empty");
    }
    if (schemaContext == null || schemaContext.isEmpty()) {
      throw new IllegalArgumentException("schemaContext must not be empty");
    }

    String contextBlock = buildSchemaContext(schemaContext);

    // Guardrailed prompt: single read-only query, no semicolon, no explanation.
    String prompt =
        """
        You are an expert PostgreSQL SQL generator.

        SCHEMA CONTEXT:
        %s

        USER REQUEST: "%s"

        RULES:
        - Return ONLY one query.
        - It MUST be read-only: start with SELECT or WITH and ultimately SELECT.
        - Absolutely NO INSERT, UPDATE, DELETE, MERGE, UPSERT, DROP, ALTER, CREATE, TRUNCATE,
          VACUUM, ANALYZE, GRANT, REVOKE, CALL, DO, COPY, LISTEN/NOTIFY, SET, EXPLAIN.
        - Use schema-qualified names if present in the context (e.g., gtw.*, bs.*).
        - Do NOT include a trailing semicolon.
        - Do NOT include any explanation or markdown.
        - Return the SQL only.
        - Unless explicitly asked for all rows, ALWAYS add `LIMIT 10` at the end of the query.
        """
            .formatted(contextBlock, userPrompt);

    // Call ChatClient; model selection is taken from application.yml
    ChatClient chatClient = chatClientBuilder.build();
    String llmOut = chatClient.prompt().user(prompt).call().content();

    String sql = sanitizeSql(llmOut);

    if (!isReadOnlySql(sql)) {
      log.warn("Rejected non-read-only SQL: {}", sql);
      throw new ReadOnlyViolationException(
          "Generated SQL is not read-only. Only SELECT/WITH queries are allowed.");
    }

    return sql;
  }

  @Override
  public String generateCotSqlWithLlm(String userPrompt, CoTDecisionResult decision) {

    List<String> schemaContext = decision.fullSchemaContext();

    if (!StringUtils.hasText(userPrompt)) {
      throw new IllegalArgumentException("userPrompt must not be empty");
    }
    if (schemaContext == null || schemaContext.isEmpty()) {
      throw new IllegalArgumentException("schemaContext must not be empty");
    }

    String contextBlock = schemaContext.toString();

    // Guardrailed prompt: single read-only query, no semicolon, no explanation.
    String prompt =
        """
            You are an expert PostgreSQL SQL generator.

            SCHEMA CONTEXT:
            %s

            USER REQUEST: "%s"

            RULES:
            - Return ONLY one query.
            - It MUST be read-only: start with SELECT or WITH and ultimately SELECT.
            - Absolutely NO INSERT, UPDATE, DELETE, MERGE, UPSERT, DROP, ALTER, CREATE, TRUNCATE,
              VACUUM, ANALYZE, GRANT, REVOKE, CALL, DO, COPY, LISTEN/NOTIFY, SET, EXPLAIN.
            - Use ONLY the tables, columns, and relationships explicitly defined in the SCHEMA CONTEXT.
              * If a requested field is not present in the SCHEMA CONTEXT, IGNORE it.
            - Use schema-qualified names if present in the context (e.g., gtw.*, bs.*).
            - Do NOT include a trailing semicolon.
            - Do NOT include any explanation or markdown.
            - Return the SQL only.
            - LIMIT POLICY:
              * For queries returning multiple detail rows (listing records), add `LIMIT 10`
                unless the user explicitly requests all rows.
              * For GROUP BY queries, add `LIMIT 10` to prevent excessive results.
              * For pure aggregate queries (using COUNT, SUM, AVG, MIN, or MAX without GROUP BY),
                you do NOT need to add a LIMIT, since they return a small number of rows.
              * If the user asks for "all" rows, still keep the result bounded and safe
                (for example, by using a higher LIMIT instead of removing it completely).
            """
            .formatted(contextBlock, userPrompt);

    // Call ChatClient; model selection is taken from application.yml
    ChatClient chatClient = chatClientBuilder.build();
    String llmOut = chatClient.prompt().user(prompt).call().content();

    String sql = sanitizeSql(llmOut);
    log.debug("Generated SQL: {}", sql);

    if (!isReadOnlySql(sql)) {
      log.warn("Rejected non-read-only SQL: {}", sql);
      throw new ReadOnlyViolationException(
          "Generated SQL is not read-only. Only SELECT/WITH queries are allowed.");
    }

    return sql;
  }

  @Override
  public List<String> findRelevantJsonSchemaContext(String userPrompt, int topK) {
    if (!StringUtils.hasText(userPrompt)) {
      throw new IllegalArgumentException("userPrompt must not be empty");
    }
    int k = topK > 0 ? topK : DEFAULT_TOP_K;

    // 1) Embed the prompt using configured embedding model (no hardcoded model name).
    float[] rawVector = embeddingService.embed(userPrompt);
    if (rawVector == null || rawVector.length == 0) {
      throw new SqlGenerationException("Empty embedding vector from embedding model");
    }

    // 2) Convert to pgvector literal: [v1,v2,...,vn]
    String pgVectorLiteral = toPgVectorLiteral(rawVector);

    // 3) Similarity search via repository (uses <-> operator).
    List<KnowledgeChunk> chunks = knowledgeChunkJsonRepository.findMostRelevant(pgVectorLiteral, k);
    if (chunks == null || chunks.isEmpty()) {
      throw new SqlGenerationException("No relevant schema information found for the prompt");
    }

    // 4) Return contents only (interface requires List<String>)
    List<String> contents = new ArrayList<>(chunks.size());
    for (KnowledgeChunk c : chunks) {
      contents.add(c.getContent());
    }
    return contents;
  }

  // ---------- Helpers ----------

  private static String toPgVectorLiteral(float[] vec) {
    String joined =
        java.util.stream.IntStream.range(0, vec.length)
            .mapToDouble(i -> vec[i]) // convert float → double
            .mapToObj(Double::toString)
            .collect(Collectors.joining(","));
    return "[" + joined + "]";
  }

  private String buildSchemaContext(List<String> chunks) {
    StringBuilder sb = new StringBuilder(256);
    sb.append("RELEVANT SCHEMA:\n\n");
    for (int i = 0; i < chunks.size(); i++) {
      sb.append("-- Chunk ").append(i + 1).append(" --\n");
      sb.append(chunks.get(i)).append("\n\n");
      if (sb.length() > MAX_CONTEXT_LENGTH) {
        sb.setLength(MAX_CONTEXT_LENGTH);
        sb.append("\n... [context truncated]");
        break;
      }
    }
    return sb.toString();
  }

  /** Strip code fences, trim, and remove a trailing semicolon if present. */
  private static String sanitizeSql(String raw) {
    if (raw == null) return "";
    String s = raw.replaceAll("(?is)```sql", "").replaceAll("(?is)```", "").trim();
    if (s.endsWith(";")) {
      s = s.substring(0, s.length() - 1).trim();
    }
    return s;
  }

  /**
   * Very defensive read-only check:
   *
   * <ul>
   *   <li>Removes comments and string literals to reduce false hits.
   *   <li>Requires leading token SELECT or WITH.
   *   <li>Rejects any DML/DDL keywords if present.
   *   <li>Rejects multiple statements (any additional ';').
   * </ul>
   */
  private static boolean isReadOnlySql(String sql) {
    if (!StringUtils.hasText(sql)) return false;

    // Reject multiple statements quickly
    if (sql.contains(";")) return false;

    String cleaned = stripComments(stripStringLiterals(sql)).trim().toLowerCase(Locale.ROOT);

    if (!(cleaned.startsWith("select") || cleaned.startsWith("with"))) return false;

    // Disallowed keywords (beyond the start); word boundaries to avoid hits in identifiers
    String[] forbidden = {
      "insert",
      "update",
      "delete",
      "merge",
      "upsert",
      "drop",
      "alter",
      "create",
      "truncate",
      "vacuum",
      "analyze",
      "grant",
      "revoke",
      "call",
      "do",
      "copy",
      "listen",
      "notify",
      "set",
      "explain" // avoid leaking plans / executing explain analyze
    };
    for (String kw : forbidden) {
      if (cleaned.matches("(?s).*\\b" + kw + "\\b.*")) {
        return false;
      }
    }
    return true;
  }

  /** Remove single-line and block comments. */
  private static String stripComments(String s) {
    // -- line comments
    String noLine = s.replaceAll("(?m)--.*?$", "");
    // /* block comments */
    return noLine.replaceAll("(?s)/\\*.*?\\*/", "");
  }

  /** Remove single- and double-quoted string literals to avoid false keyword matches. */
  private static String stripStringLiterals(String s) {
    // Replace escaped quotes inside strings conservatively
    String withoutSingle = s.replaceAll("(?s)'([^'\\\\]|\\\\.|'')*'", "''");
    return withoutSingle.replaceAll("(?s)\"([^\"\\\\]|\\\\.|\"\")*\"", "\"\"");
  }
}
