package com.stellantis.lwm2m.mcp.client.cot;

import java.util.List;

/** POJO for reasoning-based SQL validation result. */
public class ValidationResult {
  private boolean is_valid;
  private List<String> issues;
  private String suggestion;

  // Default constructor for Jackson
  public ValidationResult() {}

  public ValidationResult(boolean is_valid, List<String> issues, String suggestion) {
    this.is_valid = is_valid;
    this.issues = issues;
    this.suggestion = suggestion;
  }

  public boolean isValid() {
    return is_valid;
  }

  public void setIs_valid(boolean is_valid) {
    this.is_valid = is_valid;
  }

  public List<String> getIssues() {
    return issues;
  }

  public void setIssues(List<String> issues) {
    this.issues = issues;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  @Override
  public String toString() {
    return "ValidationResult{"
        + "is_valid="
        + is_valid
        + ", issues="
        + issues
        + ", suggestion='"
        + suggestion
        + '\''
        + '}';
  }
}
