package com.example.ebank.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ebank.bot.agents.EBankAgent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@ExtendWith(MockitoExtension.class)
class EBankTelegramBotTest {

    @Mock
    private EBankAgent eBankAgent;

    @Test
    void forwardsIncomingTextToEBankAgent() {
        when(eBankAgent.chat("List all bank accounts", "telegram-77")).thenReturn("account-list");
        RecordingTelegramBot bot = new RecordingTelegramBot(eBankAgent);

        bot.onUpdateReceived(textUpdate(77L, "List all bank accounts"));

        verify(eBankAgent).chat("List all bank accounts", "telegram-77");
    }

    @Test
    void sendsAgentResponseToTheSameChat() {
        when(eBankAgent.chat("hello", "telegram-15")).thenReturn("agent-reply");
        RecordingTelegramBot bot = new RecordingTelegramBot(eBankAgent);

        bot.onUpdateReceived(textUpdate(15L, "hello"));

        assertThat(bot.replies).containsExactly(new Reply("15", "agent-reply"));
    }

    @Test
    void ignoresUpdatesWithoutText() {
        RecordingTelegramBot bot = new RecordingTelegramBot(eBankAgent);

        bot.onUpdateReceived(new Update());

        verify(eBankAgent, never()).chat(anyString(), anyString());
        assertThat(bot.replies).isEmpty();
    }

    private static Update textUpdate(long chatId, String text) {
        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setChat(chat);
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private static final class RecordingTelegramBot extends EBankTelegramBot {

        private final List<Reply> replies = new ArrayList<>();

        private RecordingTelegramBot(EBankAgent eBankAgent) {
            super("test-token", "test-bot", eBankAgent);
        }

        @Override
        protected void sendReply(String chatId, String text) {
            replies.add(new Reply(chatId, text));
        }
    }

    private record Reply(String chatId, String text) {
    }
}
