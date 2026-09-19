package com.example.ebank.bot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ebank.bot.telegram.EBankTelegramBot;
import com.zgamelogic.discord.components.DiscordBot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class MessagingAdaptersContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithoutDiscordOrTelegramTokens() {
        assertThat(context.getBeanNamesForType(DiscordBot.class)).isEmpty();
        assertThat(context.getBeanNamesForType(EBankTelegramBot.class)).isEmpty();
    }
}
