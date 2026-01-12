package com.stellantis.lwm2m.mcp.client.service;

import com.stellantis.lwm2m.mcp.client.cot.CoTDecisionResult;
import java.util.List;
import java.util.Map;

/**
 * Database service abstraction for: 1) Retrieving relevant schema embeddings from pgvector based on
 * a user prompt. 2) Generating SQL from an LLM using the prompt + retrieved context. 3) Executing
 * SQL and returning tabular results.
 */
public interface DatabaseService {

  /**
   * Executes the given SQL query and returns the result as a list of maps. Each map represents a
   * row with column names as keys.
   *
   * @param sql the SQL query string
   * @return list of rows as key-value pairs
   */
  List<Map<String, Object>> queryForList(String sql);

  /**
   * Performs a semantic search over the pgvector-backed knowledge table (e.g.,
   * gtw.knowledge_chunks) using the user's natural-language prompt. Returns the most relevant
   * chunks to use as context.
   *
   * @param userPrompt natural-language query (e.g., "What is ECU active?")
   * @param topK maximum number of chunks to retrieve (e.g., 5 or 10)
   * @return ordered list of context snippets (highest relevance first)
   */
  List<String> findRelevantSchemaContext(String userPrompt, int topK);

  /**
   * Invokes an LLM to generate an SQL statement using the user's prompt and the retrieved schema
   * context. Implementations should ensure the prompt includes guardrails (schema-only, read-only,
   * etc.) and may also validate/parse the LLM output.
   *
   * @param userPrompt the user's natural-language question
   * @param schemaContext ordered list of relevant schema/context snippets
   * @return an SQL statement ready to execute (e.g., SELECT ...)
   */
  String generateSqlWithLlm(String userPrompt, List<String> schemaContext);

  String generateCotSqlWithLlm(String userPrompt, CoTDecisionResult decision);

  /**
   * Performs a semantic search specifically targeting JSON schema chunks (e.g., JSON column
   * definitions, sample payloads, or JSON-based specifications). This can be used when the query is
   * expected to reference JSON-structured schema rather than relational tables.
   *
   * @param userPrompt natural-language query describing the JSON schema
   * @param topK maximum number of JSON schema chunks to retrieve
   * @return ordered list of JSON schema context snippets
   */
  List<String> findRelevantJsonSchemaContext(String userPrompt, int topK);
}
