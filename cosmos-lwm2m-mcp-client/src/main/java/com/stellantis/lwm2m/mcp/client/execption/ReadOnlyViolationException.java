package com.stellantis.lwm2m.mcp.client.execption;

/** Thrown when generated SQL violates the read-only contract. */
public class ReadOnlyViolationException extends SqlGenerationException {
  public ReadOnlyViolationException(String message) {
    super(message);
  }
}
