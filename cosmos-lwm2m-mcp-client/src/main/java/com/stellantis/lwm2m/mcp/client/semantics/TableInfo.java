package com.stellantis.lwm2m.mcp.client.semantics;

import java.util.List;

public record TableInfo(
    String name,
    String friendlyName,
    String description,
    List<ColumnInfo> columns,
    List<RelationshipInfo> relationships) {
  public TableInfo withRelationships(List<RelationshipInfo> rels) {
    return new TableInfo(name, friendlyName, description, columns, rels);
  }
}
