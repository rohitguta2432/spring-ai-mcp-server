package com.stellantis.lwm2m.mcp.client.config;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.SqlTypes;

public class PostgreSQLVectorDialect extends PostgreSQLDialect {
  public PostgreSQLVectorDialect() {
    super();
  }

  @Override
  protected String columnType(int sqlTypeCode) {
    if (sqlTypeCode == SqlTypes.OTHER) {
      return "vector";
    }
    return super.columnType(sqlTypeCode);
  }
}
