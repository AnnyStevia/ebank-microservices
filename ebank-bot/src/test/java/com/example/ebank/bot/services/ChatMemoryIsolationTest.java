package com.example.ebank.bot.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChatMemoryIsolationTest {

    @Autowired
    private ChatMemory chatMemory;

    @Test
    void conversationsStayIsolatedById() {
        chatMemory.add("1", new UserMessage("My name is Mohammed"));
        chatMemory.add("2", new UserMessage("My name is Amina"));

        assertThat(chatMemory.get("1"))
                .extracting(message -> message.getText())
                .containsExactly("My name is Mohammed");
        assertThat(chatMemory.get("2"))
                .extracting(message -> message.getText())
                .containsExactly("My name is Amina");
    }
}
