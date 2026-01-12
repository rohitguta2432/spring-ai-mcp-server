package com.stellantis.lwm2m.mcp.client.cot.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellantis.lwm2m.mcp.client.cot.ValidationResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * SqlValidatorAgent uses an LLM to validate whether the generated SQL query matches the user's
 * request and the provided schema context.
 *
 * <p>It performs semantic checks that go beyond deterministic read-only enforcement.
 */
@Service
public class SqlValidatorAgent {

  private static final Logger log = LoggerFactory.getLogger(SqlValidatorAgent.class);

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;

  public SqlValidatorAgent(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  /**
   * Validate a generated SQL query using reasoning.
   *
   * @param userQuery The natural language user request
   * @param sqlQuery The generated SQL query
   * @param schemaContext The schema context used during generation
   * @return ValidationResult containing isValid flag, issues, and optional suggestion
   */
  public ValidationResult validate(String userQuery, String sqlQuery, List<String> schemaContext) {
    try {
      String prompt =
          """
                    You are a SQL validation assistant.

                    TASK:
                    - Check if the SQL query matches the user's intent.
                    - Ensure only valid tables/columns from schema context are used.
                    - Identify mismatches or semantic errors.

                    USER QUERY:
                    %s

                    GENERATED SQL:
                    %s

                    SCHEMA CONTEXT:
                    %s

                    Respond ONLY in JSON with this structure:
                    {
                      "is_valid": true/false,
                      "issues": ["list of problems if any"],
                      "suggestion": "either corrected SQL if possible, or a human-readable explanation if not"
                    }
                    """
              .formatted(userQuery, sqlQuery, schemaContext);

      String response = chatClient.prompt().user(prompt).call().content();
      ValidationResult result = objectMapper.readValue(response, ValidationResult.class);

      log.info("SQL Validation result for query='{}': {}", sqlQuery, result);
      return result;
    } catch (Exception e) {
      log.error("Validation failed for sql='{}': {}", sqlQuery, e.getMessage(), e);
      // Fail safe: if validation fails, return invalid result
      return new ValidationResult(
          false, List.of("Validation process error: " + e.getMessage()), null);
    }
  }
}
