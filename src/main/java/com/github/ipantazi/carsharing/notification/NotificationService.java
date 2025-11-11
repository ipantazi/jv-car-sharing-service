package com.github.ipantazi.carsharing.notification;

import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import org.springframework.scheduling.annotation.Async;

public interface NotificationService {
    @Async
    void sendMessage(NotificationType type, NewRentalPayload newRentalPayload);

    @Async
    void sendMessage(NotificationType type, OverdueRentalPayload overdueRentalPayload);

    @Async
    void sendMessage(NotificationType type, PaymentPayload paymentPayload);

    @Async
    void sendToDefault(String message);
}
