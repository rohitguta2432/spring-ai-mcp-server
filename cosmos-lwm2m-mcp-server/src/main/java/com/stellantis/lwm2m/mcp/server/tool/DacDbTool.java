package com.stellantis.lwm2m.mcp.server.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellantis.lwm2m.mcp.server.dto.QueryResponse;
import com.stellantis.lwm2m.mcp.server.service.DatabaseService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DacDbTool {

  private static final Logger log = LoggerFactory.getLogger(DacDbTool.class);
  private static final int MAX_RESULT_ROWS = 1000;
  private static final int MAX_ERROR_MESSAGE_LENGTH = 200;

  private final DatabaseService databaseService;
  private final ObjectMapper objectMapper;

  public DacDbTool(DatabaseService databaseService) {
    this.databaseService = databaseService;
    this.objectMapper = new ObjectMapper();
  }

  @Tool(
      name = "executeDataQuery",
      description = "Execute a SQL query on the DAC database and return formatted results as JSON.")
  public String executeDataQuery(String sqlQuery) {
    final String traceId = UUID.randomUUID().toString();
    final long t0 = System.nanoTime();
    log.info(
        "traceId={} step=START tool=executeDataQuery sqlPreview='{}'",
        traceId,
        truncate(sqlQuery, 100));

    try {
      // Basic SQL validation
      if (!isValidSql(sqlQuery)) {
        String error = "Invalid SQL query format";
        log.warn("traceId={} step=VALIDATION_ERROR error='{}'", traceId, error);
        return formatErrorResponse(error);
      }

      // Execute the query
      List<Map<String, Object>> results = databaseService.executeGeneratedSql(sqlQuery);

      // Check result size limits
      if (results.size() > MAX_RESULT_ROWS) {
        String warning =
            String.format(
                "Query returned %d rows, showing first %d rows only",
                results.size(), MAX_RESULT_ROWS);
        log.warn(
            "traceId={} step=RESULT_TRUNCATED totalRows={} maxRows={}",
            traceId,
            results.size(),
            MAX_RESULT_ROWS);
        results = results.subList(0, MAX_RESULT_ROWS);

        String response = formatSuccessResponse(results, warning);
        log.info(
            "traceId={} step=SUCCESS rows={} truncated=true totalTimeMs={}",
            traceId,
            results.size(),
            toMs(System.nanoTime() - t0));
        return response;
      }

      // Format successful response
      String response = formatSuccessResponse(results, null);
      log.info(
          "traceId={} step=SUCCESS rows={} totalTimeMs={}",
          traceId,
          results.size(),
          toMs(System.nanoTime() - t0));
      return response;

    } catch (Exception e) {
      String errorMessage = truncate(e.getMessage(), MAX_ERROR_MESSAGE_LENGTH);
      log.error(
          "traceId={} step=ERROR message='{}' exceptionType={} totalTimeMs={}",
          traceId,
          errorMessage,
          e.getClass().getSimpleName(),
          toMs(System.nanoTime() - t0),
          e);
      return formatErrorResponse("Failed to execute query: " + errorMessage);
    }
  }

  // ---------- Helper Methods ----------

  private boolean isValidSql(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return false;
    }

    String trimmed = sql.trim().toLowerCase();

    // Basic SQL validation - starts with common SQL keywords
    return trimmed.startsWith("select")
        || trimmed.startsWith("with")
        || trimmed.startsWith("show")
        || trimmed.startsWith("describe")
        || trimmed.startsWith("explain");
  }

  private String formatSuccessResponse(List<Map<String, Object>> results, String warning) {
    try {
      QueryResponse response = new QueryResponse();
      response.success = true;
      response.rowCount = results.size();
      response.data = results;
      response.warning = warning;

      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize results to JSON", e);
      return formatErrorResponse("Failed to format query results");
    }
  }

  private String formatErrorResponse(String errorMessage) {
    try {
      QueryResponse response = new QueryResponse();
      response.success = false;
      response.error = errorMessage;
      response.rowCount = 0;

      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      // Fallback to simple string if JSON serialization fails
      return "{\"success\":false,\"error\":\"" + errorMessage.replace("\"", "\\\"") + "\"}";
    }
  }

  private static String truncate(String str, int maxLength) {
    if (str == null || str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + "...";
  }

  private static long toMs(long nanos) {
    return nanos / 1_000_000L;
  }
}
