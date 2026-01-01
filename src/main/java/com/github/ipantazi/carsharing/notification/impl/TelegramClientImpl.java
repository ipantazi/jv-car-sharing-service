package com.github.ipantazi.carsharing.notification.impl;

import com.github.ipantazi.carsharing.notification.TelegramClient;
import com.github.ipantazi.carsharing.notification.dto.TelegramMessageRequest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
public class TelegramClientImpl implements TelegramClient {
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    private final String botToken;
    private final String chatId;

    public TelegramClientImpl(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.chat.id}") String chatId,
            WebClient.Builder builder,
            RateLimiter telegramRateLimiter
    ) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalArgumentException("Telegram bot token cannot be null or blank.");
        }
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("Telegram chat id cannot be null or blank.");
        }
        this.botToken = botToken;
        this.chatId = chatId;
        this.rateLimiter = telegramRateLimiter;
        this.webClient = builder.baseUrl("https://api.telegram.org").build();
    }

    @Override
    public void sendMessage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        Runnable sendTask = RateLimiter.decorateRunnable(rateLimiter, () -> doSend(text));
        try {
            sendTask.run();
        } catch (Exception ex) {
            log.warn("Telegram message sending skipped or delayed due to rate limiting", ex);
        }
    }

    private void doSend(String text) {
        String url = String.format("/bot%s/sendMessage", botToken);
        TelegramMessageRequest request = TelegramMessageRequest.html(chatId, text);

        try {
            webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException.TooManyRequests ex) {
            log.warn("Telegram rate limit exceeded (429). Message dropped.");
        } catch (Exception ex) {
            log.error("Failed to send Telegram message", ex);
        }
    }
}
