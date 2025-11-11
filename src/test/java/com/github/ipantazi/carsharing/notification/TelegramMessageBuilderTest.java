package com.github.ipantazi.carsharing.notification;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_PAYMENT_WITH_ID_101;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestNewRentalPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestOverdueRentalPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPaymentPayload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import com.github.ipantazi.carsharing.notification.impl.TelegramMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TelegramMessageBuilderTest {
    private TelegramMessageBuilder messageBuilder;

    @BeforeEach
    void setUp() {
        messageBuilder = new TelegramMessageBuilder();
    }

    @Test
    @DisplayName("Should build NEW_RENTAL_CREATED message correctly")
    public void buildMessage_NewRentalCreated_BuildNewRentalMessage() {
        // Given
        NewRentalPayload payload = createTestNewRentalPayload(EXISTING_RENTAL_ID);

        // When
        String message = messageBuilder.buildMessage(NotificationType.NEW_RENTAL_CREATED, payload);

        // Then
        assertThat(message)
                .contains("📦 <b>NEW RENTAL CREATED:</b>")
                .contains("• Rental id: %d".formatted(payload.rentalId()))
                .contains("• User: %s (%s %s)"
                        .formatted(payload.email(), payload.firstName(), payload.lastName()))
                .contains("• Car: %s %s (%s)"
                        .formatted(payload.carBrand(), payload.carModel(), payload.carType()))
                .contains("• Period: %s → %s"
                        .formatted(payload.rentalDate(), payload.returnDate()));
    }

    @Test
    @DisplayName("Should build OVERDUE_RENTAL message correctly")
    public void buildMessage_OverdueRental_BuildOverdueRentalMessage() {
        // Given
        OverdueRentalPayload payload = createTestOverdueRentalPayload(EXISTING_RENTAL_ID);

        // When
        String message = messageBuilder.buildMessage(NotificationType.OVERDUE_RENTAL, payload);

        // Then
        assertThat(message)
                .contains("⚠️ <b>OVERDUE RENTAL:</b>")
                .contains("• Rental id: %d".formatted(payload.rentalId()))
                .contains("• User: %s".formatted(payload.email()))
                .contains("• Car: %s %s (%s)"
                        .formatted(payload.carBrand(), payload.carModel(), payload.carType()))
                .contains("• Due date: %s".formatted(payload.returnDate()))
                .contains("• Days overdue: %d".formatted(payload.daysOverdue()));
    }

    @Test
    @DisplayName("Should build PAYMENT_SUCCESSFUL message correctly")
    public void buildMessage_PaymentSuccessful_BuildPaymentMessage() {
        // Given
        PaymentPayload payload = createTestPaymentPayload(EXISTING_PAYMENT_WITH_ID_101);

        // When
        String message = messageBuilder.buildMessage(NotificationType.PAYMENT_SUCCESSFUL, payload);

        // Then
        assertThat(message)
                .contains("💸 <b>PAYMENT RECEIVED:</b>")
                .contains("• Payment id: %d".formatted(payload.paymentId()))
                .contains("• Rental id: %d".formatted(payload.rentalId()))
                .contains("• User: %s".formatted(payload.email()))
                .contains("• Amount: $%s".formatted(payload.amount()))
                .contains("• Type: %s".formatted(payload.type()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when type is null")
    public void buildMessage_TypeIsNull_ThrowException() {
        // Given
        NewRentalPayload payload = createTestNewRentalPayload(EXISTING_RENTAL_ID);

        // Then & When
        assertThatThrownBy(() -> messageBuilder.buildMessage(null, payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification type cannot be null");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when type is invalid")
    public void buildMessage_InvalidType_ThrowException() {
        // Given
        NewRentalPayload payload = createTestNewRentalPayload(EXISTING_RENTAL_ID);

        // Then & When
        assertThatThrownBy(() -> messageBuilder.buildMessage(
                NotificationType.OVERDUE_RENTAL,
                payload
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported notification type: %s".formatted(
                        NotificationType.OVERDUE_RENTAL));
    }
}
