package com.github.ipantazi.carsharing.notification;

import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;

public interface NotificationMessageBuilder {
    String buildMessage(NotificationType type, NewRentalPayload rentalPayload);

    String buildMessage(NotificationType type, OverdueRentalPayload rentalPayload);

    String buildMessage(NotificationType type, PaymentPayload paymentPayload);
}
