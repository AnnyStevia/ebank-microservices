package com.example.ebank.bot.discord;

import com.example.ebank.bot.agents.EBankAgent;
import com.zgamelogic.discord.annotations.DiscordController;
import com.zgamelogic.discord.annotations.DiscordMapping;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordController
@RequiredArgsConstructor
public class DiscordBotController {

    private final EBankAgent eBankAgent;

    @DiscordMapping
    public void onMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        String content = event.getMessage().getContentRaw();
        if (content == null || content.isBlank()) {
            return;
        }
        String response = eBankAgent.chat(content, "discord-" + event.getChannel().getId());
        if (response != null && !response.isBlank()) {
            event.getChannel().sendMessage(response).queue();
        }
    }
}
