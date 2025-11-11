package com.github.ipantazi.carsharing.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessageRequest(
        @JsonProperty("chat_id")
        String chatId,

        @JsonProperty("text")
        String text,

        @JsonProperty("parse_mode")
        String parseMode
) {
    public static TelegramMessageRequest html(String chatId, String text) {
        return new TelegramMessageRequest(chatId, text, "HTML");
    }
}
