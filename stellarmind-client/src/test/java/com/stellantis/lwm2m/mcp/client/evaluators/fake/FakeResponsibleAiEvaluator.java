package com.stellantis.lwm2m.mcp.client.evaluators.fake;

import java.util.Collections;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

/**
 * A fake implementation of a Responsible AI evaluator used for unit tests.
 *
 * <p>This evaluator checks whether a generated SQL query is "safe" by applying a simple heuristic:
 * the SQL must not contain dangerous operations such as {@code DELETE} or {@code DROP}. If such
 * operations are found, the SQL is flagged as unsafe.
 *
 * <p>Purpose:
 *
 * <ul>
 *   <li>Simulates the Responsible AI (safety) dimension in a simplified way.
 *   <li>Ensures test coverage for scenarios where unsafe SQL needs to be detected.
 *   <li>Provides deterministic results for repeatable testing without requiring complex analysis.
 * </ul>
 *
 * <p><strong>Note:</strong> This class is intended for test code only (e.g., under {@code
 * src/test/java}) and should not be used in production.
 */
public class FakeResponsibleAiEvaluator {

  /**
   * Evaluates whether the SQL is safe according to simple rules.
   *
   * @param req the {@link EvaluationRequest} containing the SQL query, user text, and context
   * @return an {@link EvaluationResponse} marking the SQL as "Safe" or "Unsafe"
   */
  public EvaluationResponse evaluate(EvaluationRequest req) {
    String sql = req.getResponseContent().toLowerCase();

    // Flag as unsafe if SQL contains destructive operations
    boolean safe = !(sql.contains("delete") || sql.contains("drop"));

    // Return a response with pass/fail and feedback message
    return new EvaluationResponse(safe, safe ? "Safe" : "Unsafe", Collections.emptyMap());
  }
}
