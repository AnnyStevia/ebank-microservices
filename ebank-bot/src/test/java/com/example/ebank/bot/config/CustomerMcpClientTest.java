package com.example.ebank.bot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpStreamableHttpClientProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerMcpClientTest {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ToolCallbackProvider mcpToolCallbackProvider;

    @Autowired
    private SyncMcpToolCallbackProvider syncMcpToolCallbackProvider;

    @Autowired
    private McpClientCommonProperties mcpClientProperties;

    @Autowired
    private McpStreamableHttpClientProperties streamableHttpProperties;

    @Test
    void contextLoadsMcpClientsAgainstCustomerAndEbankServices() {
        assertThat(chatClient).isNotNull();
        assertThat(mcpToolCallbackProvider).isSameAs(syncMcpToolCallbackProvider);
        assertThat(mcpClientProperties.getType()).isEqualTo(McpClientCommonProperties.ClientType.SYNC);
        assertThat(mcpClientProperties.getToolcallback().isEnabled()).isTrue();

        McpStreamableHttpClientProperties.ConnectionParameters customerService =
                streamableHttpProperties.getConnections().get("customer-service");
        assertThat(customerService).isNotNull();
        assertThat(customerService.url()).isEqualTo("http://localhost:8056");
        assertThat(customerService.endpoint()).isEqualTo("/mcp");

        McpStreamableHttpClientProperties.ConnectionParameters ebankService =
                streamableHttpProperties.getConnections().get("ebank-service");
        assertThat(ebankService).isNotNull();
        assertThat(ebankService.url()).isEqualTo("http://localhost:8057");
        assertThat(ebankService.endpoint()).isEqualTo("/mcp");
    }

    @Test
    void chatMemoryRemainsAvailableWithMcpClient() {
        chatMemory.add("mcp-memory", new UserMessage("My name is Mohammed"));

        assertThat(chatMemory.get("mcp-memory"))
                .extracting(message -> message.getText())
                .containsExactly("My name is Mohammed");
    }
}
