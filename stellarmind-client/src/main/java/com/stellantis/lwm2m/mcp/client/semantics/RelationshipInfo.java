package com.stellantis.lwm2m.mcp.client.semantics;

public record RelationshipInfo(
    String fromTable, String fromColumn, String toTable, String toColumn) {}
