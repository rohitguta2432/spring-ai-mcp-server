package com.stellantis.lwm2m.mcp.client.cot;

import com.stellantis.lwm2m.mcp.client.cot.agent.IntentContextAgent;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

/**
 * {@code IntentEntityExtractorImpl} now acts as a thin orchestration layer that delegates query
 * understanding and reasoning to the {@link IntentContextAgent}.
 *
 * <p>Instead of directly invoking the LLM, it reuses the context-aware agent to perform both intent
 * extraction and follow-up detection, returning a unified {@link QueryAnalysis}.
 */
@Service
public class IntentEntityExtractorImpl implements IntentEntityExtractor {

  private static final Logger log = LoggerFactory.getLogger(IntentEntityExtractorImpl.class);

  private final IntentContextAgent intentContextAgent;

  public IntentEntityExtractorImpl(IntentContextAgent intentContextAgent) {
    this.intentContextAgent = intentContextAgent;
  }

  /**
   * Delegates intent and entity extraction to {@link IntentContextAgent}.
   *
   * @param userQuery latest user input
   * @return {@link QueryAnalysis} returned by the agent
   */
  @Override
  public QueryAnalysis analyze(String userQuery) {
    try {
      // empty history when no conversation context is provided
      QueryAnalysis result = intentContextAgent.analyze(userQuery, List.of());

      log.info(
          "✅ [IntentEntityExtractorImpl] Result → intent={}, entities={}, filters={}, followUp={}",
          result.intent(),
          result.entities(),
          result.filters(),
          result.isFollowUp());

      return result;

    } catch (Exception e) {
      log.error(
          "❌ [IntentEntityExtractorImpl] Agent call failed for query='{}' → {}",
          userQuery,
          e.getMessage(),
          e);
      return new QueryAnalysis("SELECT", List.of(), Map.of(), e.getMessage());
    }
  }

  /**
   * Overloaded variant for cases where prior conversation history is available.
   *
   * @param userQuery user's natural language query
   * @param history conversation history (oldest → newest)
   * @return {@link QueryAnalysis} with reasoning continuity
   */
  public QueryAnalysis analyze(String userQuery, List<Message> history) {
    try {
      QueryAnalysis result = intentContextAgent.analyze(userQuery, history);

      log.info(
          "✅ [IntentEntityExtractorImpl] Contextual Result → intent={}, schema={}, table={}, followUp={}",
          result.intent(),
          result.schema(),
          result.table(),
          result.isFollowUp());

      return result;

    } catch (Exception e) {
      log.error(
          "❌ [IntentEntityExtractorImpl] Contextual analysis failed for query='{}': {}",
          userQuery,
          e.getMessage(),
          e);
      return new QueryAnalysis("SELECT", List.of(), Map.of(), e.getMessage());
    }
  }
}
