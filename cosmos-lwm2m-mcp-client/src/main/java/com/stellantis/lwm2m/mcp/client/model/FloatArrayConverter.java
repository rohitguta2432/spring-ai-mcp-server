package com.stellantis.lwm2m.mcp.client.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Converter
public class FloatArrayConverter implements AttributeConverter<Float[], String> {

  @Override
  public String convertToDatabaseColumn(Float[] floats) {
    if (floats == null) return null;
    return Arrays.stream(floats).map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
  }

  @Override
  public Float[] convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) return new Float[0];
    dbData = dbData.replace("[", "").replace("]", "");
    String[] parts = dbData.split(",");
    Float[] floats = new Float[parts.length];
    for (int i = 0; i < parts.length; i++) {
      floats[i] = Float.parseFloat(parts[i]);
    }
    return floats;
  }
}
