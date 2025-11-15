package com.github.ipantazi.carsharing.notification;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_PAYMENT_WITH_ID_101;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TELEGRAM_MESSAGE_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestNewRentalPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestOverdueRentalPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPaymentPayload;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import com.github.ipantazi.carsharing.notification.impl.TelegramNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TelegramNotificationServiceTest {
    @Mock
    private TelegramClient telegramClient;

    @Mock
    private NotificationMessageBuilder messageBuilder;

    @InjectMocks
    private TelegramNotificationService notificationService;

    @Test
    @DisplayName("Should send message to telegram with type NEW_RENTAL_CREATED")
    public void sendMessage_NewRentalCreated_BuildAndSend() {
        // Given
        NotificationType type = NotificationType.NEW_RENTAL_CREATED;
        NewRentalPayload newRentalPayload = createTestNewRentalPayload(EXISTING_RENTAL_ID);

        when(messageBuilder.buildMessage(type, newRentalPayload)).thenReturn(TELEGRAM_MESSAGE_TEST);

        // When
        notificationService.sendMessage(type, newRentalPayload);

        // Then
        verify(messageBuilder, times(1)).buildMessage(type, newRentalPayload);
        verify(telegramClient, times(1)).sendMessage(TELEGRAM_MESSAGE_TEST);
        verifyNoMoreInteractions(messageBuilder, telegramClient);
    }

    @Test
    @DisplayName("Should send message to telegram with type OVERDUE_RENTAL")
    public void sendMessage_OverdueRental_BuildAndSend() {
        // Given
        NotificationType type = NotificationType.OVERDUE_RENTAL;
        OverdueRentalPayload overdueRentalPayload = createTestOverdueRentalPayload(
                EXISTING_RENTAL_ID);

        when(messageBuilder.buildMessage(type, overdueRentalPayload))
                .thenReturn(TELEGRAM_MESSAGE_TEST);

        // When
        notificationService.sendMessage(type, overdueRentalPayload);

        // Then
        verify(messageBuilder, times(1)).buildMessage(type, overdueRentalPayload);
        verify(telegramClient, times(1)).sendMessage(TELEGRAM_MESSAGE_TEST);
        verifyNoMoreInteractions(messageBuilder, telegramClient);
    }

    @Test
    @DisplayName("Should send message to telegram with type PAYMENT_SUCCESSFUL")
    public void sendMessage_PaymentSuccessful_BuildAndSend() {
        // Given
        NotificationType type = NotificationType.PAYMENT_SUCCESSFUL;
        PaymentPayload paymentPayload = createTestPaymentPayload(EXISTING_PAYMENT_WITH_ID_101);

        when(messageBuilder.buildMessage(type, paymentPayload)).thenReturn(TELEGRAM_MESSAGE_TEST);

        // When
        notificationService.sendMessage(type, paymentPayload);

        // Then
        verify(messageBuilder, times(1)).buildMessage(type, paymentPayload);
        verify(telegramClient, times(1)).sendMessage(TELEGRAM_MESSAGE_TEST);
        verifyNoMoreInteractions(messageBuilder, telegramClient);
    }

    @Test
    @DisplayName("Should call TelegramClient directly if no type is provided")
    public void sendToDefault_NoTypeProvided_CallTelegramClientDirectly() {
        // When
        notificationService.sendToDefault(TELEGRAM_MESSAGE_TEST);

        // Then
        verify(telegramClient, times(1)).sendMessage(TELEGRAM_MESSAGE_TEST);
        verifyNoMoreInteractions(telegramClient);
        verifyNoInteractions(messageBuilder);
    }
}
