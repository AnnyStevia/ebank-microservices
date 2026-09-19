package com.example.ebank.bot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;

class ChatClientConfigTest {

    @Test
    void wiresMemoryAdvisorAndMcpToolCallbacksOnExistingBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ToolCallbackProvider mcpTools = mock(ToolCallbackProvider.class);
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        when(builder.defaultAdvisors(any(Advisor.class))).thenReturn(builder);
        when(builder.defaultToolCallbacks(mcpTools)).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        ChatClient result = new ChatClientConfig().chatClient(builder, chatMemory, mcpTools);

        assertThat(result).isSameAs(chatClient);
        verify(builder).defaultAdvisors(any(Advisor.class));
        verify(builder).defaultToolCallbacks(mcpTools);
        verify(builder).build();
    }
}
