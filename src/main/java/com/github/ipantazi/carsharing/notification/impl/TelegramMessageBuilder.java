package com.github.ipantazi.carsharing.notification.impl;

import com.github.ipantazi.carsharing.notification.NotificationMessageBuilder;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageBuilder implements NotificationMessageBuilder {
    @Override
    public String buildMessage(NotificationType type, NewRentalPayload rentalPayload) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (type == NotificationType.NEW_RENTAL_CREATED) {
            return String.format(
                    "📦 <b>NEW RENTAL CREATED:</b>\n"
                            + "• Rental id: %d\n"
                            + "• User: %s (%s %s)\n"
                            + "• Car: %s %s (%s)\n"
                            + "• Period: %s → %s\n",
                    rentalPayload.rentalId(),
                    rentalPayload.email(),
                    rentalPayload.firstName(),
                    rentalPayload.lastName(),
                    rentalPayload.carBrand(),
                    rentalPayload.carModel(),
                    rentalPayload.carType(),
                    rentalPayload.rentalDate(),
                    rentalPayload.returnDate()
            );
        } else {
            throw new IllegalArgumentException("Unsupported notification type: %s".formatted(type));
        }
    }

    @Override
    public String buildMessage(NotificationType type, OverdueRentalPayload rentalPayload) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (type == NotificationType.OVERDUE_RENTAL) {
            return String.format(
                    "⚠️ <b>OVERDUE RENTAL:</b>\n"
                            + "• Rental id: %d\n"
                            + "• User: %s\n"
                            + "• Car: %s %s (%s)\n"
                            + "• Due date: %s\n"
                            + "• Days overdue: %d",
                    rentalPayload.rentalId(),
                    rentalPayload.email(),
                    rentalPayload.carBrand(),
                    rentalPayload.carModel(),
                    rentalPayload.carType(),
                    rentalPayload.returnDate(),
                    rentalPayload.daysOverdue()
            );
        } else {
            throw new IllegalArgumentException("Unsupported notification type: %s".formatted(type));
        }
    }

    @Override
    public String buildMessage(NotificationType type, PaymentPayload paymentPayload) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (type == NotificationType.PAYMENT_SUCCESSFUL) {
            return String.format(
                    "💸 <b>PAYMENT RECEIVED:</b>\n"
                            + "• Payment id: %d\n"
                            + "• Rental id: %d\n"
                            + "• User: %s\n"
                            + "• Amount: $%s\n"
                            + "• Type: %s",
                    paymentPayload.paymentId(),
                    paymentPayload.rentalId(),
                    paymentPayload.email(),
                    paymentPayload.amount(),
                    paymentPayload.type()
            );
        }
        throw new IllegalArgumentException("Unsupported notification type: %s".formatted(type));
    }
}
