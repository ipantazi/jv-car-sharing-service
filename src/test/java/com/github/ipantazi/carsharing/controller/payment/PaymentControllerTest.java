package com.github.ipantazi.carsharing.controller.payment;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_PAYMENT_WITH_ID_101;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_SESSION_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_SESSION_URL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_PAYMENT_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_SESSION_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYMENT_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYMENT_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createNewTestPaymentResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createNewTestPaymentResponseDtoTypeFine;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPaymentResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertCollectionsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationError;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationErrorList;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.BAD_REQUEST;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.CONFLICT;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.FORBIDDEN;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.NOT_FOUND;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.OK;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.PAYMENT_CANCEL_MESSAGE;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.PAYMENT_PAID_SUCCESS_MESSAGE;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.PAYMENT_PENDING_SUCCESS_MESSAGE;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.UNAUTHORIZED;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_PAYMENTS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_PAYMENTS_CANCEL;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_PAYMENTS_RENEW;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_PAYMENTS_SUCCESS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.createRequestWithPageable;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parsePageContent;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parseResponseToObject;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.toJson;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static com.github.ipantazi.carsharing.util.controller.MockMvcUtil.buildMockMvc;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createJsonMvcResult;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createMvcResult;
import static com.github.ipantazi.carsharing.util.controller.SecurityTestUtil.authenticateTestUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.PaymentResponseDto;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.User;
import java.util.List;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentControllerTest {
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll(@Autowired DataSource dataSource,
                                 @Autowired WebApplicationContext applicationContext) {
        mockMvc = buildMockMvc(applicationContext);

        teardown(dataSource);
        executeSqlScript(
                dataSource,
                "database/users/insert-test-users.sql",
                "database/cars/insert-test-cars.sql",
                "database/rentals/insert-test-rentals.sql",
                "database/payments/insert-test-payments.sql");
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
    @DisplayName("All payments with the given user ID and manager role should be returned.")
    void getPayments_RoleManagerAndValidUserId_ReturnsAllPaymentsWithGivenUserId()
            throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        PaymentResponseDto expectedPaymentDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<PaymentResponseDto> expectedPaymentDtoList = List.of(expectedPaymentDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE)
                        .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isOk()
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<PaymentResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualPaymentDtoList,
                expectedPaymentDtoList,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                PAYMENT_PAGEABLE,
                expectedPaymentDtoList.size()
        );
    }

    @Test
    @DisplayName("Test get payments with manager role and invalid user id")
    void getPayments_RoleManagerAndInvalidUserId_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE)
                        .param("user_id", String.valueOf(NOT_EXISTING_USER_ID)),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Can't find user with id: " + NOT_EXISTING_USER_ID
        );
    }

    @Test
    @DisplayName("Test get payments with manager role and when user id is null")
    void getPayments_RoleManagerAndUserIdNull_ShouldReturnAllPayments() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        PaymentResponseDto expectedPaymentDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<PaymentResponseDto> expectedPaymentDtoList = List.of(expectedPaymentDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE),
                status().isOk()
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<PaymentResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualPaymentDtoList,
                expectedPaymentDtoList,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                PAYMENT_PAGEABLE,
                expectedPaymentDtoList.size()
        );
    }

    @Test
    @DisplayName("""
    All payments with the customer role should be returned, ignoring the user ID specified
     in the request.
            """)
    void getPayments_RoleCustomer_ReturnsAllPaymentsWithCustomerRole() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentResponseDto expectedPaymentDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<PaymentResponseDto> expectedPaymentDtoList = List.of(expectedPaymentDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE)
                        .param("user_id", String.valueOf(EXISTING_ID_ANOTHER_USER)),
                status().isOk()
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<PaymentResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualPaymentDtoList,
                expectedPaymentDtoList,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                PAYMENT_PAGEABLE,
                expectedPaymentDtoList.size()
        );
    }

    @Test
    @DisplayName("All payments with the customer role should be returned, when user ID is null.")
    void getPayments_RoleCustomerAndUserIdNull_ReturnsAllPaymentsWithCustomerRole()
            throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentResponseDto expectedPaymentDto = createTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PAID
        );
        List<PaymentResponseDto> expectedPaymentDtoList = List.of(expectedPaymentDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE),
                status().isOk()
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<PaymentResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualPaymentDtoList,
                expectedPaymentDtoList,
                PAYMENT_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                PAYMENT_PAGEABLE,
                expectedPaymentDtoList.size()
        );
    }

    @Test
    @DisplayName("Should return empty list when there are no payments")
    @Sql(scripts = "classpath:database/payments/clear-all-payments.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/insert-test-payments.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getPayments_RoleCustomerAndNoPaymentsExist_ReturnsEmptyList() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE)
                        .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isOk()
        );

        // Then
        List<PaymentResponseDto> actualPaymentDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<PaymentResponseDto>>() {
                }
        );
        assertThat(actualPaymentDtoList).isEmpty();
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to get payments")
    void getPayments_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_PAYMENTS, PAYMENT_PAGEABLE)
                        .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isUnauthorized()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test create payment when payment does not exists")
    @Sql(scripts = "classpath:database/payments/clear-all-payments.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/payments/clear-all-payments.sql",
            "classpath:database/payments/insert-test-payments.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPayment_PaymentDoesNotExist_ReturnPaymentRequestDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentResponseDto expectedPayment = createNewTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isCreated(),
                jsonRequest
        );

        // Then
        PaymentResponseDto actualPayment = parseResponseToObject(
                result,
                objectMapper,
                PaymentResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualPayment,
                expectedPayment,
                NEW_PAYMENT_IGNORING_FIELDS
        );
        assertThat(actualPayment.getSessionUrl()).isNotBlank();
        assertThat(actualPayment.getSessionId()).isNotBlank();
    }

    @Test
    @DisplayName("Test create payment when payment is expired")
    @Sql(scripts = {"classpath:database/payments/set-status-expired-for-payment-id101.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/restoring-payment-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPayment_PaymentExpired_UpdatesPaymentWithNewSession() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentResponseDto expectedPayment = createNewTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isCreated(),
                jsonRequest
        );

        // Then
        PaymentResponseDto actualPayment = parseResponseToObject(
                result,
                objectMapper,
                PaymentResponseDto.class
        );

        assertObjectsAreEqualIgnoringFields(
                actualPayment,
                expectedPayment,
                NEW_PAYMENT_IGNORING_FIELDS
        );
        assertThat(actualPayment.getSessionUrl()).isNotBlank();
        assertThat(actualPayment.getSessionId()).isNotBlank();
    }

    @Test
    @DisplayName("Test createPayment when payment already exists with PENDING status")
    @Sql(scripts = {"classpath:database/payments/set-status-pending-for-payment-id101.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/restoring-payment-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPayment_PaymentAlreadyExistsAndPending_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "There is already a pending payment for rental: %d and type: %s. "
                        .formatted(paymentRequestDto.rentalId(), paymentRequestDto.type())
                        + "Please complete your session by url: %s"
                        .formatted(EXISTING_SESSION_URL)
        );
    }

    @Test
    @DisplayName("Test createPayment when payment already exists with PAID status")
    void createPayment_PaymentAlreadyExistsAndPaid_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Payment for rental: %d and type: %s already paid"
                        .formatted(paymentRequestDto.rentalId(), paymentRequestDto.type())
        );
    }

    @Test
    @DisplayName("Test createPayment with non existing rental")
    void createPayment_NonExistingRental_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                NOT_EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isNotFound(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Rental not found with id: %d and user id: %d."
                        .formatted(paymentRequestDto.rentalId(), EXISTING_USER_ID)
        );
    }

    @Test
    @DisplayName("Test createPayment when rental id is null")
    void createPayment_RentalIdIsNull_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                null,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'rentalId': Rental ID cannot be null")
        );
    }

    @Test
    @DisplayName("Test createPayment when rental id is negative")
    void createPayment_RentalIdIsNegative_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                (long) -1,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'rentalId': Rental ID must be a positive number")
        );
    }

    @Test
    @DisplayName("Test createPayment when type is null")
    void createPayment_TypeIsNull_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                null
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of(
                        "Field 'type': Type cannot be null",
                        "Field 'type': Type cannot be blank"
                )
        );
    }

    @Test
    @DisplayName("Test createPayment when type is blank")
    void createPayment_TypeIsBlank_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                ""
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'type': Type cannot be blank")
        );
    }

    @Test
    @DisplayName("Test create payment with type FINE when payment does not exists")
    @Sql(scripts = {"classpath:database/rentals/set-actual-rental-date-for-rental-id101.sql",
            "classpath:database/rentals/set-return-date-for-rental-id101.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/rentals/restoring-rental-id101.sql",
            "classpath:database/payments/clear-all-payments.sql",
            "classpath:database/payments/insert-test-payments.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPayment_PaymentTypeFine_ReturnPaymentRequestDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentResponseDto expectedPayment = createNewTestPaymentResponseDtoTypeFine(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.FINE)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isCreated(),
                jsonRequest
        );

        // Then
        PaymentResponseDto actualPayment = parseResponseToObject(
                result,
                objectMapper,
                PaymentResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualPayment,
                expectedPayment,
                NEW_PAYMENT_IGNORING_FIELDS
        );
        assertThat(actualPayment.getSessionUrl()).isNotBlank();
        assertThat(actualPayment.getSessionId()).isNotBlank();
    }

    @Test
    @DisplayName("Test create payment with type FINE when rental is not returned")
    void createPayment_NoNeedToPayFine_RentalIsNotReturned_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.FINE)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Rental is not returned"
        );
    }

    @Test
    @DisplayName("Test create payment with type FINE when rental is not late")
    @Sql(scripts = "classpath:database/rentals/set-actual-rental-date-for-rental-id101.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/rentals/restoring-rental-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPayment_NoNeedToPayFine_RentalIsNotLate_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.FINE)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Rental is not late"
        );
    }
    
    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to create a payment")
    void createPayment_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return Forbidden when non-CUSTOMER tries to create a payment")
    void createPayment_NonCustomer_ShouldReturnForbidden() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS),
                status().isForbidden(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("Test handleSuccess with session status PENDING")
    @Sql(scripts = {"classpath:database/payments/set-status-pending-for-payment-id101.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/restoring-payment-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void handleSuccess_WithPendingSession_ShouldReturnSuccessMessage() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_PAYMENTS_SUCCESS).param("session_id", EXISTING_SESSION_ID),
                status().isOk()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(OK);
        assertThat(result.getResponse().getContentAsString())
                .isEqualTo(PAYMENT_PENDING_SUCCESS_MESSAGE);
    }

    @Test
    @DisplayName("Test handleSuccess with session status PAID")
    void handleSuccess_WithPaidSession_ShouldReturnSuccessMessage() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_PAYMENTS_SUCCESS).param("session_id", EXISTING_SESSION_ID),
                status().isOk()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(OK);
        assertThat(result.getResponse().getContentAsString())
                .isEqualTo(PAYMENT_PAID_SUCCESS_MESSAGE);
    }

    @Test
    @DisplayName("Test handleSuccess with not existing session ID")
    void handleSuccess_WithNotExistingSessionId_ShouldReturnNotFound() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_PAYMENTS_SUCCESS).param("session_id", NOT_EXISTING_SESSION_ID),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Payment not found for session id: " + NOT_EXISTING_SESSION_ID
        );
    }

    @Test
    @DisplayName("Test handleSuccess when session id is missing")
    void handleSuccess_SessionIdMissing_ThrowsException() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_PAYMENTS_SUCCESS),
                status().isBadRequest()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("Test handleCancel")
    void handleCancel_ShouldReturnCancelMessage() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_PAYMENTS_CANCEL),
                status().isOk()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(OK);
        assertThat(result.getResponse().getContentAsString()).isEqualTo(PAYMENT_CANCEL_MESSAGE);
    }

    @Test
    @DisplayName("Test renewPaymentSession when payment is expired")
    @Sql(scripts = {"classpath:database/payments/set-status-expired-for-payment-id101.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/restoring-payment-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void renewPaymentSession_PaymentExpired_UpdatesPaymentWithNewSession() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentResponseDto expectedPayment = createNewTestPaymentResponseDto(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isCreated(),
                jsonRequest
        );

        // Then
        PaymentResponseDto actualPayment = parseResponseToObject(
                result,
                objectMapper,
                PaymentResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualPayment,
                expectedPayment,
                NEW_PAYMENT_IGNORING_FIELDS
        );
        assertThat(actualPayment.getSessionId()).isNotNull();
        assertThat(actualPayment.getSessionUrl()).isNotNull();
    }

    @Test
    @DisplayName("Test renewPaymentSession when payment does not exist")
    @Sql(scripts = "classpath:database/payments/clear-all-payments.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/payments/clear-all-payments.sql",
            "classpath:database/payments/insert-test-payments.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void renewPaymentSession_PaymentDoesNotExist_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isNotFound(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "No previous session found for this rental: %d and type: %s."
                        .formatted(paymentRequestDto.rentalId(), paymentRequestDto.type())
                        + " Please create a new payment session."
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession when payment already exists with PENDING status")
    @Sql(scripts = {"classpath:database/payments/set-status-pending-for-payment-id101.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/payments/restoring-payment-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void renewPaymentSession_PaymentExistsWithPendingStatus_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "There is already a pending payment for rental: %d and type: %s. "
                        .formatted(paymentRequestDto.rentalId(), paymentRequestDto.type())
                        + "Please complete your session by url: %s"
                        .formatted(EXISTING_SESSION_URL)
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession when payment already exists with PAID status")
    void renewPaymentSession_PaymentExistsWithPaidStatus_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Payment for rental: %d and type: %s already paid"
                        .formatted(paymentRequestDto.rentalId(), paymentRequestDto.type())
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession with non existing rental")
    void renewPaymentSession_NonExistingRental_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                NOT_EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isNotFound(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Rental not found with id: %d and user id: %d."
                        .formatted(paymentRequestDto.rentalId(), EXISTING_USER_ID)
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession when rental id is null")
    void renewPaymentSession_RentalIdIsNull_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                null,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'rentalId': Rental ID cannot be null")
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession when rental id is negative")
    void renewPaymentSession_RentalIdIsNegative_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                (long) -1,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'rentalId': Rental ID must be a positive number")
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession when type is null")
    void renewPaymentSession_TypeIsNull_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                null
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of(
                        "Field 'type': Type cannot be null",
                        "Field 'type': Type cannot be blank"
                )
        );
    }

    @Test
    @DisplayName("Test renewPaymentSession when type is blank")
    void renewPaymentSession_TypeIsBlank_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                ""
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'type': Type cannot be blank")
        );
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to renewPaymentSession")
    void renewPaymentSession_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return Forbidden when non-CUSTOMER tries to renewPaymentSession")
    void renewPaymentSession_NonCustomer_ShouldReturnForbidden() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(Payment.Type.PAYMENT)
        );
        String jsonRequest = toJson(objectMapper, paymentRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_PAYMENTS_RENEW),
                status().isForbidden(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }
}
