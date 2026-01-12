package com.stellantis.lwm2m.mcp.server.dto;

import java.util.List;
import java.util.Map;

/**
 * Represents the response structure for a query operation. This class encapsulates the success
 * status, row count, actual data, and any error or warning messages associated with the query.
 */
public class QueryResponse {
  /** Indicates whether the query operation was successful. */
  public boolean success;

  /** The number of rows returned by the query. */
  public int rowCount;

  /**
   * The actual data returned by the query, represented as a list of maps. Each map represents a
   * row, with keys as column names and values as cell data.
   */
  public List<Map<String, Object>> data;

  /**
   * An error message if the query operation failed. This field will be null or empty if the
   * operation was successful.
   */
  public String error;

  /**
   * A warning message if there were any warnings during the query operation. This field will be
   * null or empty if there were no warnings.
   */
  public String warning;

  /**
   * Gets the success status of the query operation.
   *
   * @return true if the query was successful, false otherwise.
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Sets the success status of the query operation.
   *
   * @param success true if the query was successful, false otherwise.
   */
  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * Gets the number of rows returned by the query.
   *
   * @return the row count.
   */
  public int getRowCount() {
    return rowCount;
  }

  /**
   * Sets the number of rows returned by the query.
   *
   * @param rowCount the row count.
   */
  public void setRowCount(int rowCount) {
    this.rowCount = rowCount;
  }

  /**
   * Gets the actual data returned by the query.
   *
   * @return a list of maps, where each map represents a row of data.
   */
  public List<Map<String, Object>> getData() {
    return data;
  }

  /**
   * Sets the actual data returned by the query.
   *
   * @param data a list of maps, where each map represents a row of data.
   */
  public void setData(List<Map<String, Object>> data) {
    this.data = data;
  }

  /**
   * Gets the error message associated with the query operation.
   *
   * @return the error message, or null if no error occurred.
   */
  public String getError() {
    return error;
  }

  /**
   * Sets the error message associated with the query operation.
   *
   * @param error the error message.
   */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * Gets the warning message associated with the query operation.
   *
   * @return the warning message, or null if no warning occurred.
   */
  public String getWarning() {
    return warning;
  }

  /**
   * Sets the warning message associated with the query operation.
   *
   * @param warning the warning message.
   */
  public void setWarning(String warning) {
    this.warning = warning;
  }
}
