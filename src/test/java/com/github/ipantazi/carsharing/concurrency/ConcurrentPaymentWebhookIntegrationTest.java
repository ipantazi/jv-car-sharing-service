package com.github.ipantazi.carsharing.concurrency;

import static com.github.ipantazi.carsharing.util.TestDataUtil.AMOUNT_TO_PAY_FOR_NEW_PAYMENT;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.LOCAL_HOST;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.config.BaseConcurrencyIntegrationTest;
import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.service.payment.PaymentService;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeWebhookService;
import com.github.ipantazi.carsharing.util.concurrency.ConcurrencyTestHelper;
import com.github.ipantazi.carsharing.util.stripe.TestStripeEventFactory;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

class ConcurrentPaymentWebhookIntegrationTest extends BaseConcurrencyIntegrationTest {
    private static final Payment.Type TYPE_PAYMENT = Payment.Type.PAYMENT;
    private static final Payment.Status STATUS_PAID = Payment.Status.PAID;

    @MockitoBean
    private StripeClient stripeClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private StripeWebhookService stripeWebhookService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeAll
    public static void beforeAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
        executeSqlScript(
                dataSource,
                "database/users/insert-test-users.sql",
                "database/cars/insert-test-cars.sql",
                "database/rentals/insert-test-rentals.sql"
        );
    }

    @AfterEach
    public void afterEach(@Autowired DataSource dataSource) {
        executeSqlScript(dataSource,"database/payments/clear-all-payments.sql");
    }

    @AfterAll
    public static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    public static void teardown(DataSource dataSource) {
        executeSqlScript(
                dataSource,
                "database/payments/clear-all-payments.sql",
                "database/rentals/clear-all-rentals.sql",
                "database/cars/clear-all-cars.sql",
                "database/users/clear-all-users.sql"
        );
    }

    @Test
    @DisplayName("""
            Payment creation and Stripe webhook arriving concurrently
            → payment must be idempotent and PAID only once
            """)
    void paymentCreationAndWebhookArriveConcurrently_shouldRemainIdempotent() throws Exception {
        // Given
        int threadCount = 2;
        String signature = TestStripeEventFactory.validSignature();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(LOCAL_HOST);
        ConcurrencyTestHelper helper = new ConcurrencyTestHelper(threadCount);
        PaymentRequestDto requestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                TYPE_PAYMENT.name()
        );
        String webhookPayload = TestStripeEventFactory.dummyPayload(
                EXISTING_RENTAL_ID,
                TYPE_PAYMENT,
                AMOUNT_TO_PAY_FOR_NEW_PAYMENT
        );
        var stripeTestContextHolder = TestStripeEventFactory.checkoutSessionCompleted(
                EXISTING_RENTAL_ID,
                TYPE_PAYMENT,
                AMOUNT_TO_PAY_FOR_NEW_PAYMENT
        );

        when(stripeClient.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(stripeTestContextHolder.event());
        when(stripeClient.getMetadataFromSession(stripeTestContextHolder.session()))
                .thenReturn(stripeTestContextHolder.metadataDto());

        List<Callable<String>> tasks = List.of(
                // Thread A: API creates payment session
                () -> {
                    paymentService.createPaymentSession(
                            EXISTING_USER_ID,
                            requestDto,
                            uriBuilder
                    );
                    return "CREATE_PAYMENT_OK";
                },
                // Thread B: Stripe webhook arrives concurrently (or retry)
                () -> {
                    stripeWebhookService.processStripeEvent(webhookPayload, signature);
                    return "WEBHOOK_OK";
                }
        );

        // When
        helper.runConcurrentTasks(tasks);

        // Then
        Optional<Payment> paymentOptional = paymentRepository.findPaymentByRentalIdAndType(
                EXISTING_RENTAL_ID, TYPE_PAYMENT);
        assertThat(paymentOptional)
                .as("Payment must exist")
                .isPresent();

        Payment payment = paymentOptional.get();
        assertThat(payment.getStatus())
                .as("Final payment state must be PAID")
                .isEqualTo(STATUS_PAID);

        assertThat(payment.getAmountToPay())
                .as("Amount must match Stripe metadata")
                .isEqualByComparingTo(AMOUNT_TO_PAY_FOR_NEW_PAYMENT);
    }

    @Test
    @DisplayName("""
            Stripe webhook retry must be idempotent
            → repeated webhook must not change PAID payment
            """)
    void stripeWebhookRetry_shouldBeIdempotent() throws Exception {
        // Given
        String signature = TestStripeEventFactory.validSignature();
        String payload = TestStripeEventFactory.dummyPayload(
                EXISTING_RENTAL_ID,
                TYPE_PAYMENT,
                AMOUNT_TO_PAY_FOR_NEW_PAYMENT
        );
        var stripeTestContextHolder = TestStripeEventFactory.checkoutSessionCompleted(
                EXISTING_RENTAL_ID,
                TYPE_PAYMENT,
                AMOUNT_TO_PAY_FOR_NEW_PAYMENT
        );

        when(stripeClient.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(stripeTestContextHolder.event());
        when(stripeClient.getMetadataFromSession(stripeTestContextHolder.session()))
                .thenReturn(stripeTestContextHolder.metadataDto());

        // When
        // First webhook
        stripeWebhookService.processStripeEvent(payload, signature);
        // retry webhook
        stripeWebhookService.processStripeEvent(payload, signature);

        // Then
        Optional<Payment> paymentOptional = paymentRepository.findPaymentByRentalIdAndType(
                EXISTING_RENTAL_ID,
                TYPE_PAYMENT
        );

        assertThat(paymentOptional).isPresent();
        assertThat(paymentOptional.get().getStatus()).isEqualTo(STATUS_PAID);
    }
}
