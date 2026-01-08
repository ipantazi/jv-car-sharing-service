package com.github.ipantazi.carsharing.repository.payment;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_PAYMENT_WITH_ID_101;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYMENT_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYMENT_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPayment;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.config.BaseJpaIntegrationTest;
import com.github.ipantazi.carsharing.model.Payment;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {
        "classpath:database/cars/insert-test-cars.sql",
        "classpath:database/users/insert-test-users.sql",
        "classpath:database/rentals/insert-test-rentals.sql",
        "classpath:database/payments/insert-test-payments.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = {
        "classpath:database/payments/clear-all-payments.sql",
        "classpath:database/rentals/clear-all-rentals.sql",
        "classpath:database/users/clear-all-users.sql",
        "classpath:database/cars/clear-all-cars.sql"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public class PaymentRepositoryTest extends BaseJpaIntegrationTest {
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Test findPaymentsByUserId() method exists payments by user id")
    void findPaymentsByUserId_ExistsPaymentsByUserId_ReturnsPageOfPayments() {
        // Given
        Payment expextedPayment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<Payment> paymentList = Collections.singletonList(expextedPayment);
        Page<Payment> expectedPaymentPage = new PageImpl<>(
                paymentList,
                PAYMENT_PAGEABLE,
                paymentList.size()
        );

        // When
        Page<Payment> actualPaymentPage = paymentRepository.findPaymentsByUserId(
                EXISTING_USER_ID,
                PAYMENT_PAGEABLE
        );

        // Then
        List<Payment> actualPaymentList = actualPaymentPage.getContent();
        assertThat(actualPaymentList).isNotEmpty().hasSize(1);
        assertObjectsAreEqualIgnoringFields(
                actualPaymentList.get(0),
                expextedPayment,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(actualPaymentPage, expectedPaymentPage);
    }

    @Test
    @DisplayName("Test findPaymentsByUserId() method not exists payments by user id")
    @Sql(scripts = "classpath:database/payments/clear-all-payments.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/insert-test-payments.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findPaymentsByUserId_NotExistsPaymentsByUserId_ReturnsEmptyPage() {
        // Given
        Page<Payment> expectedPaymentPage = new PageImpl<>(
                Collections.emptyList(),
                PAYMENT_PAGEABLE,
                0
        );

        // When
        Page<Payment> actualPaymentPage = paymentRepository.findPaymentsByUserId(
                EXISTING_USER_ID,
                PAYMENT_PAGEABLE
        );

        // Then
        assertPageMetadataEquals(actualPaymentPage, expectedPaymentPage);
        assertThat(actualPaymentPage.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Test findPaymentsByUserId() method with not exists user id")
    void findPaymentsByUserId_NotExistsUserId_ReturnsLong() {
        // Given
        Page<Payment> expectedPaymentPage = new PageImpl<>(
                Collections.emptyList(),
                PAYMENT_PAGEABLE,
                0
        );

        // When
        Page<Payment> actualPaymentPage = paymentRepository.findPaymentsByUserId(
                NOT_EXISTING_USER_ID,
                PAYMENT_PAGEABLE
        );

        // Then
        assertPageMetadataEquals(actualPaymentPage, expectedPaymentPage);
        assertThat(actualPaymentPage.getContent()).isEmpty();
    }

    @Test
    @DisplayName(
            "Test existsByUserIdAndStatus() method with exists payments by user id and status"
    )
    void existsByUserIdAndStatus_ExistsPaymentsByUserIdAndStatus_ReturnsLong() {
        // Given
        Long expectedResult = 1L;

        // When
        Long actualResult = paymentRepository.existsByUserIdAndStatus(
                EXISTING_USER_ID,
                String.valueOf(Payment.Status.PAID)
        );

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName(
            "Test existsByUserIdAndStatus() method with not exists payments by user id and status"
    )
    @Sql(scripts = "classpath:database/payments/clear-all-payments.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/insert-test-payments.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void existsByUserIdAndStatus_NotExistsPaymentsByUserIdAndStatus_ReturnsLong() {
        // Given
        Long expectedResult = 0L;

        // When
        Long actualResult = paymentRepository.existsByUserIdAndStatus(
                NOT_EXISTING_USER_ID,
                String.valueOf(Payment.Status.PAID)
        );

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName(
            "Test existsByUserIdAndStatus() method with not exists user id"
    )
    void existsByUserIdAndStatus_NotExistsUserId_ReturnsLong() {
        // Given
        Long expectedResult = 0L;

        // When
        Long actualResult = paymentRepository.existsByUserIdAndStatus(
                NOT_EXISTING_USER_ID,
                String.valueOf(Payment.Status.PAID)
        );

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName(
            "Test existsByUserIdAndStatus() method with not exists payments by status EXPIRED"
    )
    void existsByUserIdAndStatus_NotExistsPaymentsByStatusExpired_ReturnsLong() {
        // Given
        Long expectedResult = 0L;

        // When
        Long actualResult = paymentRepository.existsByUserIdAndStatus(
                EXISTING_USER_ID,
                String.valueOf(Payment.Status.EXPIRED)
        );

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }
}
