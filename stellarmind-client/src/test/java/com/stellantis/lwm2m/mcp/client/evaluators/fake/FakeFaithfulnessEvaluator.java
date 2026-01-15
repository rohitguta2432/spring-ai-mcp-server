package com.stellantis.lwm2m.mcp.client.evaluators.fake;

import java.util.Collections;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

/**
 * A fake implementation of a FaithfulnessEvaluator used for unit tests.
 *
 * <p>This evaluator checks whether the generated SQL query is "faithful" to the schema context
 * provided in the {@link EvaluationRequest}. It does this by verifying that at least one referenced
 * table name from the context appears in the SQL string.
 *
 * <p>Purpose:
 *
 * <ul>
 *   <li>Simulates the faithfulness dimension of evaluation without requiring real LLM-based
 *       evaluators.
 *   <li>Flags SQL that references tables not present in the context as "Hallucinated".
 *   <li>Ensures deterministic results during testing.
 * </ul>
 *
 * <p><strong>Note:</strong> This class is intended for test code only (e.g., under {@code
 * src/test/java}) and should not be used in production.
 */
public class FakeFaithfulnessEvaluator {

  /**
   * Evaluates whether the SQL is faithful to the provided context.
   *
   * @param req the {@link EvaluationRequest} containing the SQL query, user text, and schema
   *     context
   * @return an {@link EvaluationResponse} indicating whether the SQL is faithful ("Faithful") or
   *     hallucinated ("Hallucinated")
   */
  public EvaluationResponse evaluate(EvaluationRequest req) {
    String sql = req.getResponseContent().toLowerCase();

    // Check if any table name from the provided documents appears in the SQL
    boolean faithful =
        req.getDataList().stream()
            .anyMatch(
                doc -> {
                  String text = doc.getText().toLowerCase();
                  // Extract table name (first token after "table ")
                  String tableName = text.replace("table ", "").split(" ")[0];
                  return sql.contains(tableName);
                });

    // Return a pass if a matching table is found; otherwise mark as hallucinated
    return new EvaluationResponse(
        faithful, faithful ? "Faithful" : "Hallucinated", Collections.emptyMap());
  }
}
