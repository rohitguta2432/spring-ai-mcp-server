package com.stellantis.lwm2m.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(McpClientApplication.class, args);
  }

  @Bean
  @Qualifier("nexusconnect-mcp-server-callback-tool-provider")
  public SyncMcpToolCallbackProvider toolCallbackProvider(List<McpSyncClient> mcpSyncClients) {
    return new SyncMcpToolCallbackProvider(mcpSyncClients);
  }
}
