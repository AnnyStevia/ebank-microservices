package com.example.ebank.bot.telegram;

import com.example.ebank.bot.agents.EBankAgent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@ConditionalOnExpression("!'${telegram.bot.token:}'.isBlank() && '${telegram.bot.enabled:true}' != 'false'")
public class EBankTelegramBot extends TelegramLongPollingBot {

    private final String username;
    private final EBankAgent eBankAgent;

    public EBankTelegramBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username:EBANK-BOT}") String username,
            EBankAgent eBankAgent) {
        super(token);
        this.username = username;
        this.eBankAgent = eBankAgent;
    }

    @PostConstruct
    public void start() throws TelegramApiException {
        new TelegramBotsApi(DefaultBotSession.class).registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String text = update.getMessage().getText();
        if (text.isBlank()) {
            return;
        }
        String chatId = String.valueOf(update.getMessage().getChatId());
        String response = eBankAgent.chat(text, "telegram-" + chatId);
        if (response == null || response.isBlank()) {
            return;
        }
        try {
            sendReply(chatId, response);
        } catch (TelegramApiException ex) {
            log.warn("Failed to send Telegram reply to chat {}", chatId, ex);
        }
    }

    protected void sendReply(String chatId, String text) throws TelegramApiException {
        execute(new SendMessage(chatId, text));
    }
}
