package com.stellantis.lwm2m.mcp.client.cot;

// service/IntentEntityExtractor.java

import java.util.List;
import org.springframework.ai.chat.messages.Message;

public interface IntentEntityExtractor {
  QueryAnalysis analyze(String userQuery);

  QueryAnalysis analyze(String userQuery, List<Message> history);
}
