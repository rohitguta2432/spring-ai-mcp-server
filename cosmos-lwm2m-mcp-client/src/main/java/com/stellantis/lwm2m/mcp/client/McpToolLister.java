package com.stellantis.lwm2m.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class McpToolLister implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(McpToolLister.class);

  private final List<McpSyncClient> mcpClients;

  @Autowired
  public McpToolLister(List<McpSyncClient> mcpClients) {
    this.mcpClients = mcpClients;
  }

  @Override
  public void run(String... args) {
    log.info("Discovering MCP Tools...");
    for (McpSyncClient client : mcpClients) {
      log.info("  Connected to MCP Client: {}", client.getClientInfo().name());

      // Use the provider to get ToolCallbacks from the client
      SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(List.of(client));
      List<ToolCallback> toolCallbacks = List.of(provider.getToolCallbacks());

      if (toolCallbacks.isEmpty()) {
        log.warn("    No tools found on this MCP client.");
      } else {
        for (ToolCallback toolCallback : toolCallbacks) {
          ToolDefinition toolDefinition = toolCallback.getToolDefinition();
          log.info("    Tool Name: {}", toolDefinition.name());
          log.debug("      Description: {}", toolDefinition.description());
          log.debug("      Input Schema: {}", toolDefinition.inputSchema());
          log.info("--------------------");
        }
      }
    }
  }
}
