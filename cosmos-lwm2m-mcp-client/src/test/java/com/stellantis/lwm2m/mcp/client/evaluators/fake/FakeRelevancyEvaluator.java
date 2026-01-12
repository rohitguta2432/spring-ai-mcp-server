package com.stellantis.lwm2m.mcp.client.evaluators.fake;

import java.util.Collections;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

/**
 * A fake implementation of a RelevancyEvaluator used for unit tests.
 *
 * <p>This evaluator checks whether the generated SQL query is "relevant" to the schema context
 * provided in the {@link EvaluationRequest}. It does this by ensuring that the SQL string
 * references at least one of the table names mentioned in the context documents.
 *
 * <p>Purpose:
 *
 * <ul>
 *   <li>Simulates the relevancy dimension of evaluation in a simplified way.
 *   <li>Flags SQL that does not reference any known table as "Not relevant".
 *   <li>Provides deterministic results for repeatable unit testing.
 * </ul>
 *
 * <p><strong>Note:</strong> This class is intended for test code only (e.g., under {@code
 * src/test/java}) and should not be used in production.
 */
public class FakeRelevancyEvaluator {

  /**
   * Evaluates whether the SQL is relevant to the provided schema context.
   *
   * @param req the {@link EvaluationRequest} containing the SQL query, user text, and schema
   *     context
   * @return an {@link EvaluationResponse} indicating whether the SQL is relevant ("Relevant") or
   *     not ("Not relevant")
   */
  public EvaluationResponse evaluate(EvaluationRequest req) {
    String sql = req.getResponseContent().toLowerCase();

    // Extract table names from the context documents and check if SQL mentions them
    boolean relevant =
        req.getDataList().stream()
            .map(Document::getText) // document text like "Table bs.bs_ecu (...)"
            .map(String::toLowerCase) // normalize
            .map(t -> t.replace("table ", "").split(" ")[0]) // extract "bs.bs_ecu"
            .anyMatch(sql::contains); // check if SQL contains the table name

    // Return "Relevant" if a table is matched, otherwise "Not relevant"
    return new EvaluationResponse(
        relevant, relevant ? "Relevant" : "Not relevant", Collections.emptyMap());
  }
}
