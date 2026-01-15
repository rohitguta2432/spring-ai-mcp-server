package com.stellantis.lwm2m.mcp.client.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PgVectorFloatArrayConverter implements AttributeConverter<float[], String> {
  @Override
  public String convertToDatabaseColumn(float[] attribute) {
    if (attribute == null) return null;
    StringBuilder sb = new StringBuilder(attribute.length * 8 + 2);
    sb.append('[');
    for (int i = 0; i < attribute.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(Float.toString(attribute[i]));
    }
    sb.append(']');
    return sb.toString();
  }

  @Override
  public float[] convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) return new float[0];
    String s = dbData.trim();
    if (s.startsWith("[")) s = s.substring(1);
    if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
    if (s.isBlank()) return new float[0];
    String[] parts = s.split("\\s*,\\s*");
    float[] out = new float[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Float.parseFloat(parts[i]);
    return out;
  }
}
