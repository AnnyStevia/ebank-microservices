package com.example.ebank.bot.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * The Discord starter always logs in when {@code DiscordBot} is created.
 * Skip that auto-configuration unless a real token is supplied so REST /chat
 * and tests can start without Discord.
 */
public class OptionalDiscordEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";
    static final String DISCORD_BOT_AUTOCONFIG = "com.zgamelogic.discord.components.DiscordBot";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String token = environment.getProperty("discord.token");
        if (token != null && !token.isBlank()) {
            return;
        }
        String existing = environment.getProperty(EXCLUDE_PROPERTY);
        String value = (existing == null || existing.isBlank())
                ? DISCORD_BOT_AUTOCONFIG
                : existing + "," + DISCORD_BOT_AUTOCONFIG;
        environment.getPropertySources().addFirst(
                new MapPropertySource("optionalDiscord", Map.of(EXCLUDE_PROPERTY, value)));
    }
}
