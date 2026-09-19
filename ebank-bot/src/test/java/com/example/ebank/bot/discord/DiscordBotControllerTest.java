package com.example.ebank.bot.discord;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ebank.bot.agents.EBankAgent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscordBotControllerTest {

    @Mock
    private EBankAgent eBankAgent;

    @Mock
    private MessageReceivedEvent event;

    @Mock
    private User author;

    @Mock
    private Message message;

    @Mock
    private MessageChannelUnion channel;

    private DiscordBotController controller;

    @BeforeEach
    void setUp() {
        controller = new DiscordBotController(eBankAgent);
    }

    @Test
    void ignoresBotGeneratedMessages() {
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(true);

        controller.onMessage(event);

        verify(eBankAgent, never()).chat(anyString(), anyString());
        verify(event, never()).getChannel();
    }

    @Test
    void forwardsUserMessageToEBankAgent() {
        stubUserMessage("List all customers", "42");
        when(eBankAgent.chat("List all customers", "discord-42")).thenReturn("customer-list");
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage("customer-list")).thenReturn(action);

        controller.onMessage(event);

        verify(eBankAgent).chat("List all customers", "discord-42");
    }

    @Test
    void sendsAgentResponseToTheSameChannel() {
        stubUserMessage("hello", "99");
        when(eBankAgent.chat("hello", "discord-99")).thenReturn("agent-reply");
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage("agent-reply")).thenReturn(action);

        controller.onMessage(event);

        verify(channel).sendMessage("agent-reply");
        verify(action).queue();
    }

    private void stubUserMessage(String content, String channelId) {
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn(content);
        when(event.getChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn(channelId);
    }
}
