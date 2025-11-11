package com.github.ipantazi.carsharing.notification;

public interface TelegramClient {
    void sendMessage(String text);
}
