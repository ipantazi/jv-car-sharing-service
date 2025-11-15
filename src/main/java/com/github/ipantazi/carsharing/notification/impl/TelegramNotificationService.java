package com.github.ipantazi.carsharing.notification.impl;

import com.github.ipantazi.carsharing.notification.NotificationMessageBuilder;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.TelegramClient;
import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramNotificationService implements NotificationService {
    private final TelegramClient telegramClient;
    private final NotificationMessageBuilder messageBuilder;

    @Async
    @Override
    public void sendMessage(NotificationType type, NewRentalPayload newRentalPayload) {
        String message = messageBuilder.buildMessage(type, newRentalPayload);
        telegramClient.sendMessage(message);
    }

    @Async
    @Override
    public void sendMessage(NotificationType type, OverdueRentalPayload overdueRentalPayload) {
        String message = messageBuilder.buildMessage(type, overdueRentalPayload);
        telegramClient.sendMessage(message);
    }

    @Async
    @Override
    public void sendMessage(NotificationType type, PaymentPayload paymentPayload) {
        String message = messageBuilder.buildMessage(type, paymentPayload);
        telegramClient.sendMessage(message);
    }

    @Override
    public void sendToDefault(String message) {
        telegramClient.sendMessage(message);
    }
}
