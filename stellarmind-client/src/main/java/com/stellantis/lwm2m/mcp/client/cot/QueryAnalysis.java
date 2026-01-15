package com.stellantis.lwm2m.mcp.client.cot;

import java.util.List;
import java.util.Map;

/**
 * Unified record representing both semantic and contextual understanding of a user query.
 *
 * <p>It captures everything the LLM or parsing layer infers about a query — including intent,
 * entities, filters, and conversational continuity.
 *
 * <p>Used by both {@code IntentEntityExtractorImpl} and {@code IntentContextAgent}.
 */
public record QueryAnalysis(

    /** Query intent — e.g. SELECT, UPDATE, DELETE, etc. */
    String intent,

    /** Entities or tables referenced in the query. */
    List<String> entities,

    /** Explicit key-value filters mentioned by the user. */
    Map<String, String> filters,

    /** Error message or clarification hint if query is incomplete. */
    String error,

    /** Whether this query continues from the previous context. */
    boolean isFollowUp,

    /** Schema inferred for reuse (null if new query). */
    String schema,

    /** Table inferred for reuse (null if new query). */
    String table,

    /** Updated or final SQL if refinement was inferred. */
    String finalSql,

    /** Short reasoning or explanation of the agent’s decision. */
    String reasoning) {

  /** Convenience constructor for basic intent/entity extraction (no context reasoning). */
  public QueryAnalysis(
      String intent, List<String> entities, Map<String, String> filters, String error) {
    this(intent, entities, filters, error, false, null, null, null, null);
  }
}
