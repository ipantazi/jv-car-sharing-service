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
        validateNotificationType(type);

        if (type == NotificationType.NEW_RENTAL_CREATED) {
            return String.format(
                    "📦 <b>NEW RENTAL CREATED:</b>\n"
                            + "• Rental id: %d\n"
                            + "• User: %s (%s %s)\n"
                            + "• Car: %s %s (%s)\n"
                            + "• Period: %s → %s\n",
                    rentalPayload.rentalId(),
                    escapeHtmlDynamic(rentalPayload.email()),
                    escapeHtmlDynamic(rentalPayload.firstName()),
                    escapeHtmlDynamic(rentalPayload.lastName()),
                    escapeHtmlDynamic(rentalPayload.carBrand()),
                    escapeHtmlDynamic(rentalPayload.carModel()),
                    escapeHtmlDynamic(rentalPayload.carType()),
                    rentalPayload.rentalDate(),
                    rentalPayload.returnDate()
            );
        } else {
            throw new IllegalArgumentException("Unsupported notification type: %s".formatted(type));
        }
    }

    @Override
    public String buildMessage(NotificationType type, OverdueRentalPayload rentalPayload) {
        validateNotificationType(type);

        if (type == NotificationType.OVERDUE_RENTAL) {
            return String.format(
                    "⚠️ <b>OVERDUE RENTAL:</b>\n"
                            + "• Rental id: %d\n"
                            + "• User: %s\n"
                            + "• Car: %s %s (%s)\n"
                            + "• Due date: %s\n"
                            + "• Days overdue: %d",
                    rentalPayload.rentalId(),
                    escapeHtmlDynamic(rentalPayload.email()),
                    escapeHtmlDynamic(rentalPayload.carBrand()),
                    escapeHtmlDynamic(rentalPayload.carModel()),
                    escapeHtmlDynamic(rentalPayload.carType()),
                    rentalPayload.returnDate(),
                    rentalPayload.daysOverdue()
            );
        } else {
            throw new IllegalArgumentException("Unsupported notification type: %s".formatted(type));
        }
    }

    @Override
    public String buildMessage(NotificationType type, PaymentPayload paymentPayload) {
        validateNotificationType(type);

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
                    escapeHtmlDynamic(paymentPayload.email()),
                    paymentPayload.amount(),
                    paymentPayload.type()
            );
        }
        throw new IllegalArgumentException("Unsupported notification type: %s".formatted(type));
    }

    private String escapeHtmlDynamic(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void validateNotificationType(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
    }
}
