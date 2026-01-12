package com.stellantis.lwm2m.mcp.client.evaluators.fake;

import java.util.Collections;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

/**
 * A fake implementation of a FactCheckingEvaluator used for unit tests.
 *
 * <p>This class bypasses any real fact-checking logic. Instead, it always returns a passing {@link
 * EvaluationResponse} with the feedback "Correct".
 *
 * <p>Purpose:
 *
 * <ul>
 *   <li>Allows SQL generation tests to run without invoking real LLM-based evaluators.
 *   <li>Ensures predictable outcomes for test verification (always true).
 *   <li>Encapsulates "fact checking" as one of the evaluation dimensions in the test framework.
 * </ul>
 *
 * <p><strong>Note:</strong> This class should only be used in test code under {@code
 * src/test/java}. It is not intended for production.
 */
public class FakeFactCheckingEvaluator {

  /**
   * Evaluates the given request.
   *
   * @param req the {@link EvaluationRequest} containing user text, context, and SQL response
   * @return an {@link EvaluationResponse} that always passes with static feedback
   */
  public EvaluationResponse evaluate(EvaluationRequest req) {
    // Always return a passing result to simulate fact-checking success
    return new EvaluationResponse(true, "Correct", Collections.emptyMap());
  }
}
