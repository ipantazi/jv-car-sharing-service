package com.github.ipantazi.carsharing.service.payment;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_PAYMENT_WITH_ID_101;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_SESSION_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_PAYMENT_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYMENT_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYMENT_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createNewTestPaymentResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPayment;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPaymentPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPaymentResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestStripeSessionMetadataDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.PaymentResponseDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.PaymentAlreadyPaidException;
import com.github.ipantazi.carsharing.exception.PendingPaymentsExistException;
import com.github.ipantazi.carsharing.mapper.PaymentMapper;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.notification.NotificationMapper;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.service.payment.impl.PaymentServiceImpl;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import com.github.ipantazi.carsharing.service.rental.RentalService;
import com.github.ipantazi.carsharing.service.user.UserService;
import com.stripe.exception.StripeException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private RentalService rentalService;
    @Mock
    private Calculator calculator;
    @Mock
    private StripeClient stripeClient;
    @Mock
    private PaymentValidator paymentValidator;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private UserService userService;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Get payments with existing user id")
    public void getPayments_UserId_ReturnsPageOfPaymentsResponseDto() {
        // Given
        PaymentResponseDto expectedPaymentDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<Payment> paymentList = Collections.singletonList(payment);
        Page<Payment> paymentPage = new PageImpl<>(
                paymentList,
                PAYMENT_PAGEABLE,
                paymentList.size()
        );

        when(paymentRepository.findPaymentsByUserId(EXISTING_USER_ID, PAYMENT_PAGEABLE))
                .thenReturn(paymentPage);
        when(paymentMapper.toPaymentResponseDto(payment)).thenReturn(expectedPaymentDto);

        // When
        Page<PaymentResponseDto> actualPaymentPage = paymentService.getPayments(
                EXISTING_USER_ID,
                PAYMENT_PAGEABLE
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = actualPaymentPage.getContent();
        assertThat(actualPaymentDtoList).isNotEmpty().hasSize(1);
        assertObjectsAreEqualIgnoringFields(
                actualPaymentDtoList.get(0),
                expectedPaymentDto,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(actualPaymentPage, paymentPage);

        verify(paymentRepository, times(1))
                .findPaymentsByUserId(EXISTING_USER_ID, PAYMENT_PAGEABLE);
        verify(paymentMapper, times(1)).toPaymentResponseDto(payment);
        verifyNoMoreInteractions(paymentRepository, paymentMapper);
    }

    @Test
    @DisplayName("No existing payments by this user id")
    public void getPayments_NoPaymentsByUserId_ReturnsEmptyPage() {
        // Given
        Page<Payment> paymentPage = new PageImpl<>(
                Collections.emptyList(),
                PAYMENT_PAGEABLE,
                0
        );

        when(paymentRepository.findPaymentsByUserId(EXISTING_ID_ANOTHER_USER, PAYMENT_PAGEABLE))
                .thenReturn(paymentPage);

        // When
        Page<PaymentResponseDto> actualPaymentPage = paymentService.getPayments(
                EXISTING_ID_ANOTHER_USER,
                PAYMENT_PAGEABLE
        );

        // Then
        assertPageMetadataEquals(actualPaymentPage, paymentPage);
        assertThat(actualPaymentPage.getContent()).isEmpty();

        verify(paymentRepository, times(1))
                .findPaymentsByUserId(EXISTING_ID_ANOTHER_USER, PAYMENT_PAGEABLE);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Test getPayments() method when user id is null")
    public void getPayments_UserIdIsNull_ReturnsPageOfPaymentResponseDto() {
        // Given
        PaymentResponseDto expectedPaymentDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<Payment> paymentList = Collections.singletonList(payment);
        Page<Payment> paymentPage = new PageImpl<>(
                paymentList,
                PAYMENT_PAGEABLE,
                paymentList.size()
        );

        when(paymentRepository.findAll(PAYMENT_PAGEABLE))
                .thenReturn(paymentPage);
        when(paymentMapper.toPaymentResponseDto(payment)).thenReturn(expectedPaymentDto);

        // When
        Page<PaymentResponseDto> actualPaymentPage = paymentService.getPayments(
                null,
                PAYMENT_PAGEABLE
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = actualPaymentPage.getContent();
        assertThat(actualPaymentDtoList).isNotEmpty().hasSize(1);
        assertObjectsAreEqualIgnoringFields(
                actualPaymentDtoList.get(0),
                expectedPaymentDto,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(actualPaymentPage, paymentPage);

        verify(paymentRepository, times(1))
                .findAll(PAYMENT_PAGEABLE);
        verify(paymentMapper, times(1)).toPaymentResponseDto(payment);
        verifyNoMoreInteractions(paymentRepository, paymentMapper);
    }

    @Test
    @DisplayName("Test createPaymentSession() method when payment does not exist")
    public void createPaymentSession_PaymentDoesNotExist_ReturnsPaymentResponseDto()
            throws StripeException {
        // Given
        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
                "http://localhost");
        PaymentResponseDto expectedPaymentResponseDto = createNewTestPaymentResponseDto(
                NEW_PAYMENT_ID,
                Payment.Status.PENDING
        );
        Payment payment = createTestPayment(expectedPaymentResponseDto);
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                expectedPaymentResponseDto.getRentalId(),
                String.valueOf(expectedPaymentResponseDto.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        StripeSessionMetadataDto stripeSessionMetadataDto = createTestStripeSessionMetadataDto(
                expectedPaymentResponseDto);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                expectedPaymentResponseDto.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                expectedPaymentResponseDto.getRentalId(),
                expectedPaymentResponseDto.getType()
        )).thenReturn(Optional.empty());
        when(calculator.calculateAmountToPayByType(rental, expectedPaymentResponseDto.getType()))
                .thenReturn(expectedPaymentResponseDto.getAmountToPay());
        when(stripeClient.createSession(
                any(BigDecimal.class),
                anyString(),
                anyString(),
                eq(paymentRequestDto)
        )).thenReturn(stripeSessionMetadataDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toPaymentResponseDto(any(Payment.class)))
                .thenReturn(expectedPaymentResponseDto);

        // When
        PaymentResponseDto actualPaymentResponseDto = paymentService.createPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualPaymentResponseDto,
                expectedPaymentResponseDto,
                PAYMENT_IGNORING_FIELDS
        );

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                expectedPaymentResponseDto.getRentalId()
        );
        verify(paymentRepository, times(1))
                .findPaymentByRentalIdAndType(expectedPaymentResponseDto.getRentalId(),
                        expectedPaymentResponseDto.getType());
        verify(calculator, times(1))
                .calculateAmountToPayByType(rental, expectedPaymentResponseDto.getType());
        verify(stripeClient, times(1))
                .createSession(
                        any(BigDecimal.class),
                        anyString(),
                        anyString(),
                        eq(paymentRequestDto)
                );
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentMapper, times(1)).toPaymentResponseDto(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository, calculator, stripeClient);
    }

    @Test
    @DisplayName("Test createPaymentSession() method when payment is expired")
    public void createPaymentSession_PaymentIsExpired_ReturnsPaymentResponseDto()
            throws StripeException {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        PaymentResponseDto expectedPaymentResponseDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
        Payment payment = createTestPayment(EXISTING_PAYMENT_WITH_ID_101, Payment.Status.EXPIRED);
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                expectedPaymentResponseDto.getRentalId(),
                String.valueOf(expectedPaymentResponseDto.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        StripeSessionMetadataDto stripeSessionMetadataDto = createTestStripeSessionMetadataDto(
                expectedPaymentResponseDto);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                expectedPaymentResponseDto.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                expectedPaymentResponseDto.getRentalId(),
                expectedPaymentResponseDto.getType()
        )).thenReturn(Optional.of(payment));
        when(calculator.calculateAmountToPayByType(rental, expectedPaymentResponseDto.getType()))
                .thenReturn(expectedPaymentResponseDto.getAmountToPay());
        when(stripeClient.createSession(
                any(BigDecimal.class),
                anyString(),
                anyString(),
                eq(paymentRequestDto)
        )).thenReturn(stripeSessionMetadataDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toPaymentResponseDto(any(Payment.class)))
                .thenReturn(expectedPaymentResponseDto);

        // When
        PaymentResponseDto actualPaymentResponseDto = paymentService.createPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualPaymentResponseDto,
                expectedPaymentResponseDto,
                PAYMENT_IGNORING_FIELDS
        );
        assertThat(actualPaymentResponseDto.getStatus()).isEqualTo(Payment.Status.PENDING);

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                expectedPaymentResponseDto.getRentalId()
        );
        verify(paymentRepository, times(1))
                .findPaymentByRentalIdAndType(expectedPaymentResponseDto.getRentalId(),
                        expectedPaymentResponseDto.getType());
        verify(calculator, times(1))
                .calculateAmountToPayByType(rental, expectedPaymentResponseDto.getType());
        verify(stripeClient, times(1))
                .createSession(
                        any(BigDecimal.class),
                        anyString(),
                        anyString(),
                        eq(paymentRequestDto)
                );
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentMapper, times(1)).toPaymentResponseDto(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository, calculator, stripeClient);
    }

    @Test
    @DisplayName("Test createPaymentSession() method when payment is already paid")
    public void createPaymentSession_PaymentIsPaid_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                payment.getRentalId(),
                String.valueOf(payment.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                payment.getRentalId(),
                payment.getType()
        )).thenReturn(Optional.of(payment));

        // When & Then
        assertThatThrownBy(() -> paymentService.createPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        ))
                .isInstanceOf(PaymentAlreadyPaidException.class)
                .hasMessage("Payment for rental: %d and type: %s already paid"
                        .formatted(payment.getRentalId(), payment.getType()));

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        );
        verify(paymentRepository, times(1))
                .findPaymentByRentalIdAndType(payment.getRentalId(), payment.getType());
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository);
        verifyNoInteractions(calculator, stripeClient);
    }

    @Test
    @DisplayName("Test createPaymentSession() method when payment is already pending")
    public void createPaymentSession_PaymentIsPending_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                payment.getRentalId(),
                String.valueOf(payment.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                payment.getRentalId(),
                payment.getType()
        )).thenReturn(Optional.of(payment));

        // When & Then
        assertThatThrownBy(() -> paymentService.createPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        ))
                .isInstanceOf(PendingPaymentsExistException.class)
                .hasMessage("There is already a pending payment for rental: %d and type: %s. "
                        .formatted(payment.getRentalId(), payment.getType())
                        + "Please complete your session by url: %s"
                        .formatted(payment.getSessionUrl()));

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        );
        verify(paymentRepository, times(1))
                .findPaymentByRentalIdAndType(payment.getRentalId(), payment.getType());
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository);
        verifyNoInteractions(calculator, stripeClient);
    }

    @Test
    @DisplayName("Test createPaymentSession() method when rental id is not found")
    public void createPaymentSession_RentalIdIsNotFound_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                NOT_EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                NOT_EXISTING_RENTAL_ID
        )).thenThrow(new EntityNotFoundException("Rental not found with id: %d and user id: %d."
                .formatted(NOT_EXISTING_RENTAL_ID, EXISTING_USER_ID)));

        // When & Then
        assertThatThrownBy(() -> paymentService.createPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: %d and user id: %d."
                        .formatted(NOT_EXISTING_RENTAL_ID, EXISTING_USER_ID));

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                NOT_EXISTING_RENTAL_ID
        );
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService);
        verifyNoInteractions(calculator, stripeClient, paymentRepository);
    }

    @Test
    @DisplayName("Test getPaymentSuccessMessage() method with payment status PAID")
    public void getPaymentSuccessMessage_PaymentStatusPaid_ReturnsSuccessMessage() {
        // Given
        String expectedMessage = "Thank you for your payment. Payment successful.";
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        when(paymentRepository.findPaymentBySessionId(payment.getSessionId()))
                .thenReturn(Optional.of(payment));

        // When
        String actualMessage = paymentService.getPaymentSuccessMessage(payment.getSessionId());

        // Then
        assertThat(actualMessage).isEqualTo(expectedMessage);
        verify(paymentRepository, times(1)).findPaymentBySessionId(payment.getSessionId());
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Test getPaymentSuccessMessage() method with payment status PENDING")
    public void getPaymentSuccessMessage_PaymentStatusPending_ReturnsPendingMessage() {
        // Given
        String expectedMessage = "Thank you for your payment. Payment is being processed.";
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
        when(paymentRepository.findPaymentBySessionId(payment.getSessionId()))
                .thenReturn(Optional.of(payment));

        // When
        String actualMessage = paymentService.getPaymentSuccessMessage(payment.getSessionId());

        // Then
        assertThat(actualMessage).isEqualTo(expectedMessage);
        verify(paymentRepository, times(1)).findPaymentBySessionId(payment.getSessionId());
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Test getPaymentSuccessMessage() method when payment is not found")
    public void getPaymentSuccessMessage_PaymentIsNotFound_ThrowsException() {
        // Given
        when(paymentRepository.findPaymentBySessionId(INVALID_SESSION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentSuccessMessage(INVALID_SESSION_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Payment not found for session id: " + INVALID_SESSION_ID);

        verify(paymentRepository, times(1)).findPaymentBySessionId(INVALID_SESSION_ID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Test renewPaymentSession() method when payment is expired")
    public void renewPaymentSession_PaymentIsExpired_ReturnsPaymentResponseDto()
            throws StripeException {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        PaymentResponseDto expectedPaymentResponseDto = createNewTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.EXPIRED);
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                expectedPaymentResponseDto.getRentalId(),
                String.valueOf(expectedPaymentResponseDto.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        StripeSessionMetadataDto stripeSessionMetadataDto = createTestStripeSessionMetadataDto(
                expectedPaymentResponseDto);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                expectedPaymentResponseDto.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                expectedPaymentResponseDto.getRentalId(),
                expectedPaymentResponseDto.getType()
        )).thenReturn(Optional.of(payment));
        when(calculator.calculateAmountToPayByType(rental, expectedPaymentResponseDto.getType()))
                .thenReturn(expectedPaymentResponseDto.getAmountToPay());
        when(stripeClient.createSession(
                any(BigDecimal.class),
                anyString(),
                anyString(),
                eq(paymentRequestDto)
        )).thenReturn(stripeSessionMetadataDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toPaymentResponseDto(any(Payment.class)))
                .thenReturn(expectedPaymentResponseDto);

        // When
        PaymentResponseDto actualPaymentResponseDto = paymentService.renewPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualPaymentResponseDto,
                expectedPaymentResponseDto
        );
        assertThat(actualPaymentResponseDto.getStatus()).isEqualTo(Payment.Status.PAID);

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                expectedPaymentResponseDto.getRentalId()
        );
        verify(paymentRepository, times(1)).findPaymentByRentalIdAndType(
                expectedPaymentResponseDto.getRentalId(),
                expectedPaymentResponseDto.getType()
        );
        verify(calculator, times(1))
                .calculateAmountToPayByType(rental, expectedPaymentResponseDto.getType());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentMapper, times(1)).toPaymentResponseDto(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository, calculator, paymentMapper);
    }

    @Test
    @DisplayName("Test renewPaymentSession() method when payment is already PAID")
    public void renewPaymentSession_PaymentIsPaid_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID);
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                payment.getRentalId(),
                String.valueOf(payment.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                payment.getRentalId(),
                payment.getType()
        )).thenReturn(Optional.of(payment));

        // When & Then
        assertThatThrownBy(() -> paymentService.renewPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        )).isInstanceOf(PaymentAlreadyPaidException.class)
                .hasMessage("Payment for rental: %d and type: %s already paid"
                        .formatted(payment.getRentalId(), payment.getType()));

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        );
        verify(paymentRepository, times(1)).findPaymentByRentalIdAndType(
                payment.getRentalId(),
                payment.getType()
        );
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository);
        verifyNoInteractions(calculator, stripeClient, paymentMapper);
    }

    @Test
    @DisplayName("Test renewPaymentSession() method when payment has status PENDING")
    public void renewPaymentSession_PaymentIsPending_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING);
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                payment.getRentalId(),
                String.valueOf(payment.getType())
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                payment.getRentalId(),
                payment.getType()
        )).thenReturn(Optional.of(payment));

        // When & Then
        assertThatThrownBy(() -> paymentService.renewPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        )).isInstanceOf(PendingPaymentsExistException.class)
                .hasMessage("There is already a pending payment for rental: %d and type: %s. "
                        .formatted(payment.getRentalId(), payment.getType())
                        + "Please complete your session by url: %s"
                        .formatted(payment.getSessionUrl()));

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                payment.getRentalId()
        );
        verify(paymentRepository, times(1)).findPaymentByRentalIdAndType(
                payment.getRentalId(),
                payment.getType()
        );
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository);
        verifyNoInteractions(calculator, stripeClient, paymentMapper);
    }

    @Test
    @DisplayName("Test renewPaymentSession() method when payment does not exist")
    public void renewPaymentSession_PaymentDoesNotExist_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_PAYMENT_WITH_ID_101,
                String.valueOf(Payment.Type.PAYMENT)
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(rentalService.getRentalByIdAndUserId(
                EXISTING_USER_ID,
                paymentRequestDto.rentalId()
        )).thenReturn(rental);
        when(paymentRepository.findPaymentByRentalIdAndType(
                paymentRequestDto.rentalId(),
                Payment.Type.valueOf(paymentRequestDto.type())
        )).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.renewPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        )).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("No previous session found for this rental: %d and type: %s."
                        .formatted(paymentRequestDto.rentalId(), paymentRequestDto.type())
                        + " Please create a new payment session.");

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                paymentRequestDto.rentalId()
        );
        verify(paymentRepository, times(1)).findPaymentByRentalIdAndType(
                paymentRequestDto.rentalId(),
                Payment.Type.valueOf(paymentRequestDto.type())
        );
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService, paymentRepository);
        verifyNoInteractions(calculator, stripeClient, paymentMapper);
    }

    @Test
    @DisplayName("Test renewPaymentSession() method when rental id is not found")
    public void renewPaymentSession_RentalIdIsNotFound_ThrowsException() {
        // Given
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                NOT_EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        when(rentalService.getRentalByIdAndUserId(EXISTING_USER_ID, paymentRequestDto.rentalId()))
                .thenThrow(new EntityNotFoundException(
                        "Rental not found with id: %d and user id: %d."
                        .formatted(paymentRequestDto.rentalId(), EXISTING_USER_ID))
                );

        // When & Then
        assertThatThrownBy(() -> paymentService.renewPaymentSession(
                EXISTING_USER_ID,
                paymentRequestDto,
                uriBuilder
        )).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: %d and user id: %d."
                        .formatted(paymentRequestDto.rentalId(), EXISTING_USER_ID));

        verify(rentalService, times(1)).getRentalByIdAndUserId(
                EXISTING_USER_ID,
                paymentRequestDto.rentalId()
        );
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoMoreInteractions(rentalService);
        verifyNoInteractions(calculator, stripeClient, paymentMapper, paymentRepository);
    }

    @Test
    @DisplayName("Test handlePaymentSuccess() method when payment has status PENDING")
    public void handlePaymentSuccess_PaymentHasStatusPending_Success() {
        // Given
        Payment payment = createTestPayment(EXISTING_PAYMENT_WITH_ID_101, Payment.Status.PENDING);
        PaymentPayload paymentPayload = createTestPaymentPayload(payment);
        final StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(payment);

        when(paymentRepository.findPaymentBySessionId(payment.getSessionId()))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userService.getEmailByRentalId(payment.getRentalId())).thenReturn(EXISTING_EMAIL);
        when(notificationMapper.toPaymentPayload(payment, EXISTING_EMAIL))
                .thenReturn(paymentPayload);

        // When
        paymentService.handlePaymentSuccess(metadataDto);

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);

        verify(paymentRepository, times(1)).findPaymentBySessionId(payment.getSessionId());
        verify(paymentValidator, times(1)).checkingAmountToPay(metadataDto, payment);
        verify(paymentRepository, times(1)).save(payment);
        verify(userService, times(1)).getEmailByRentalId(payment.getRentalId());
        verify(notificationMapper, times(1)).toPaymentPayload(payment, EXISTING_EMAIL);
        verify(notificationService, times(1))
                .sendMessage(NotificationType.PAYMENT_SUCCESSFUL, paymentPayload);
        verifyNoMoreInteractions(paymentRepository, paymentValidator);
        verifyNoMoreInteractions(userService, notificationMapper, notificationService);
    }

    @Test
    @DisplayName("Test handlePaymentSuccess() method when payment has status EXPIRED")
    public void handlePaymentSuccess_PaymentHasStatusExpired_Success() {
        // Given
        Payment payment = createTestPayment(EXISTING_PAYMENT_WITH_ID_101, Payment.Status.EXPIRED);
        PaymentPayload paymentPayload = createTestPaymentPayload(payment);
        final StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(payment);

        when(paymentRepository.findPaymentBySessionId(payment.getSessionId()))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userService.getEmailByRentalId(payment.getRentalId())).thenReturn(EXISTING_EMAIL);
        when(notificationMapper.toPaymentPayload(payment, EXISTING_EMAIL))
                .thenReturn(paymentPayload);

        // When
        paymentService.handlePaymentSuccess(metadataDto);

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);

        verify(paymentRepository, times(1)).findPaymentBySessionId(payment.getSessionId());
        verify(paymentValidator, times(1)).checkingAmountToPay(metadataDto, payment);
        verify(paymentRepository, times(1)).save(payment);
        verify(userService, times(1)).getEmailByRentalId(payment.getRentalId());
        verify(notificationMapper, times(1)).toPaymentPayload(payment, EXISTING_EMAIL);
        verify(notificationService, times(1))
                .sendMessage(NotificationType.PAYMENT_SUCCESSFUL, paymentPayload);
        verifyNoMoreInteractions(paymentRepository, paymentValidator);
        verifyNoMoreInteractions(userService, notificationMapper, notificationService);
    }

    @Test
    @DisplayName("Test handlePaymentSuccess() method when payment has status PAID")
    public void handlePaymentSuccess_PaymentHasStatusPaid_NoAction() {
        // Given
        Payment payment = createTestPayment(EXISTING_PAYMENT_WITH_ID_101, Payment.Status.PAID);
        StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(payment);

        when(paymentRepository.findPaymentBySessionId(payment.getSessionId()))
                .thenReturn(Optional.of(payment));

        // When
        paymentService.handlePaymentSuccess(metadataDto);

        // Then
        verify(paymentRepository, times(1)).findPaymentBySessionId(payment.getSessionId());
        verify(paymentRepository, never()).save(payment);
        verifyNoMoreInteractions(paymentRepository);
        verifyNoInteractions(paymentValidator, userService);
        verifyNoInteractions(notificationMapper, notificationService);
    }

    @Test
    @DisplayName("Test handlePaymentSuccess() method when payment not found")
    public void handlePaymentSuccess_PaymentNotFound_CreatesNewPayment() {
        // Given
        Payment expectedPayment = createTestPayment(
                NEW_PAYMENT_ID,
                Payment.Status.PAID
        );
        PaymentPayload paymentPayload = createTestPaymentPayload(expectedPayment);
        final StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(
                expectedPayment);

        when(paymentRepository.findPaymentBySessionId(expectedPayment.getSessionId()))
                .thenReturn(Optional.empty());
        when(userService.getEmailByRentalId(expectedPayment.getRentalId()))
                .thenReturn(EXISTING_EMAIL);
        when(notificationMapper.toPaymentPayload(any(Payment.class), anyString()))
                .thenReturn(paymentPayload);

        // When
        paymentService.handlePaymentSuccess(metadataDto);

        // Then
        verify(paymentRepository, times(1)).findPaymentBySessionId(expectedPayment.getSessionId());
        verify(paymentValidator, times(1)).checkingAmountToPay(metadataDto, null);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        assertObjectsAreEqualIgnoringFields(
                captor.getValue(),
                expectedPayment,
                PAYMENT_IGNORING_FIELDS);

        verify(userService, times(1)).getEmailByRentalId(expectedPayment.getRentalId());
        verify(notificationMapper, times(1)).toPaymentPayload(any(Payment.class), anyString());
        verify(notificationService, times(1))
                .sendMessage(NotificationType.PAYMENT_SUCCESSFUL, paymentPayload);
        verifyNoMoreInteractions(paymentRepository, paymentValidator);
        verifyNoMoreInteractions(userService, notificationMapper, notificationService);
    }
}
