package com.github.ipantazi.carsharing.notification.impl;

import com.github.ipantazi.carsharing.notification.TelegramClient;
import com.github.ipantazi.carsharing.notification.dto.TelegramMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class TelegramClientImpl implements TelegramClient {
    private final WebClient webClient;
    private final String botToken;
    private final String chatId;

    public TelegramClientImpl(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.chat.id}") String chatId,
            WebClient.Builder builder
    ) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalArgumentException("Telegram bot token cannot be null or blank.");
        }
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("Telegram chat id cannot be null or blank.");
        }
        this.botToken = botToken;
        this.chatId = chatId;
        this.webClient = builder.baseUrl("https://api.telegram.org").build();
    }

    @Override
    public void sendMessage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String url = String.format("/bot%s/sendMessage", botToken);
        String safeText = escapeHtml(text);
        TelegramMessageRequest request = TelegramMessageRequest.html(chatId, safeText);

        webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
