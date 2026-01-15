package com.stellantis.lwm2m.mcp.client.cot.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple wrapper for a table's column list used by Text2SQL validation.
 *
 * <p>JSON format in Redis: { "columns": ["col1", "col2", ...] }
 */
public record SchemaColumns(List<String> columns) {

  public SchemaColumns {
    // Normalize to a non-null, mutable list inside the record
    if (columns == null) {
      columns = new ArrayList<>();
    } else if (!(columns instanceof ArrayList)) {
      columns = new ArrayList<>(columns);
    }
  }

  public boolean hasColumn(String columnName) {
    if (columnName == null) {
      return false;
    }
    return columns.stream().anyMatch(c -> c.equalsIgnoreCase(columnName));
  }
}
