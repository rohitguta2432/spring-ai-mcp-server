package com.stellantis.lwm2m.mcp.server.execption;

// ---------- Exceptions ----------

/** Base exception for SQL generation/execution pipeline failures. */
public class SqlGenerationException extends RuntimeException {
  public SqlGenerationException(String message) {
    super(message);
  }

  public SqlGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
