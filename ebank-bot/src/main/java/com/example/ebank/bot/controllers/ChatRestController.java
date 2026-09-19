package com.example.ebank.bot.controllers;

import com.example.ebank.bot.agents.EBankAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatRestController {

    private final EBankAgent eBankAgent;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(
            @RequestParam(defaultValue = "bonjour") String query,
            @RequestParam(defaultValue = "default") String conversationId) {
        return eBankAgent.chat(query, conversationId);
    }
}
