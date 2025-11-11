package com.github.ipantazi.carsharing.util;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.payment.PaymentResponseDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserLoginRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.security.CustomUserDetails;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.LongStream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class TestDataUtil {
    public static final int EXPECTED_CARS_SIZE = 3;
    public static final int EXPECTED_RENTALS_SIZE = 2;
    public static final Long NEGATIVE_ID = -1L;
    public static final Long EXISTING_CAR_ID = 101L;
    public static final Long ALTERNATIVE_CAR_ID = 103L;
    public static final Long SAFE_DELETED_CAR_ID = 104L;
    public static final Long NEW_CAR_ID = 105L;
    public static final Long NOT_EXISTING_CAR_ID = 999L;
    public static final Long EXISTING_USER_ID = 101L;
    public static final Long EXISTING_ID_ANOTHER_USER = 102L;
    public static final Long SAFE_DELETED_USER_ID = 103L;
    public static final Long NEW_USER_ID = 104L;
    public static final Long NOT_EXISTING_USER_ID = 999L;
    public static final Long EXISTING_RENTAL_ID = 101L;
    public static final Long EXISTING_RENTAL_ID_ANOTHER_USER = 102L;
    public static final Long NEW_RENTAL_ID = 103L;
    public static final Long NOT_EXISTING_RENTAL_ID = 999L;
    public static final Long EXISTING_PAYMENT_WITH_ID_101 = 101L;
    public static final Long EXISTING_PAYMENT_WITH_ID_102 = 102L;
    public static final Long NEW_PAYMENT_ID = 103L;
    public static final Long NOT_EXISTING_PAYMENT_ID = 999L;

    public static final String CAR_MODEL = "Test Car ";
    public static final String CAR_BRAND = "Test Brand ";
    public static final String CAR_TYPE = "UNIVERSAL";
    public static final int CAR_INVENTORY = 5;
    public static final BigDecimal CAR_DAILY_FEE = new BigDecimal("101.0")
            .setScale(1, RoundingMode.HALF_UP);
    public static final BigDecimal TEST_AMOUNT = new BigDecimal("101.0");

    public static final String EMAIL_DOMAIN = "@example.com";
    public static final String NEW_EMAIL = NEW_USER_ID + EMAIL_DOMAIN;
    public static final String EXISTING_EMAIL = EXISTING_USER_ID + EMAIL_DOMAIN;
    public static final String NOT_HASHED_PASSWORD = "Test&password1";
    public static final String NOT_EXISTING_NOT_HASHED_PASSWORD = "Not&existingPassword1";
    public static final String NEW_NOT_HASHED_PASSWORD = "New&password1";
    public static final String B_CRYPT_PASSWORD = "$2a$10$TYVQIW25Boqejv0QvAYYn.6nQHmiypul1BkRgww"
            + "1wPxSuLYBUg0f.";
    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";

    public static final int MIN_RENTAL_DAYS = 2;
    public static final int MAX_RENTAL_DAYS = 30;
    public static final int NUMBER_OF_RENTAL_DAYS = 5;
    public static final BigDecimal LATE_FEE_MULTIPLIER = BigDecimal.valueOf(1.5);
    public static final LocalDate RENTAL_DATE = LocalDate.of(2025, 1, 1);
    public static final LocalDate ACTUAL_RETURN_DATE = RENTAL_DATE.plusDays(3);
    public static final LocalDate RETURN_DATE = RENTAL_DATE.plusDays(NUMBER_OF_RENTAL_DAYS);
    public static final ZoneId ZONE = ZoneId.of("UTC");
    public static final Instant FIXED_INSTANT = RENTAL_DATE.plusDays(4).atStartOfDay(ZONE)
            .toInstant();
    public static final LocalDate FIXED_DATE = LocalDate.ofInstant(FIXED_INSTANT, ZONE);
    public static final LocalDate RETURN_DATE_BEFORE_FIXED_DATE = FIXED_DATE.minusDays(2);
    public static final LocalDate ACTUAL_RETURN_DATE_AFTER_RETURN_DATE =
            RETURN_DATE.plusDays(1);
    public static final long DAYS_OVERDUE = ChronoUnit.DAYS.between(FIXED_DATE,
            RETURN_DATE_BEFORE_FIXED_DATE);

    public static final long EXPIRY_SECONDS = 86400;
    public static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZONE);
    public static final long NOW_EPOCH = FIXED_CLOCK.instant().getEpochSecond();
    public static final long EXPIRED_CREATED_TIME = NOW_EPOCH - EXPIRY_SECONDS - 1;
    public static final long RECENT_CREATED_TIME = NOW_EPOCH - EXPIRY_SECONDS + 1;

    public static final BigDecimal AMOUNT_TO_PAY = CAR_DAILY_FEE.multiply(BigDecimal.valueOf(
            ChronoUnit.DAYS.between(RENTAL_DATE, RETURN_DATE)
    )).setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal AMOUNT_TO_PAY_FOR_NEW_PAYMENT = CAR_DAILY_FEE
            .multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(
                    FIXED_DATE,
                    FIXED_DATE.plusDays(NUMBER_OF_RENTAL_DAYS)
            )).setScale(2, RoundingMode.HALF_UP));
    public static final BigDecimal FINE_AMOUNT_TO_PAY = CAR_DAILY_FEE
            .multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(
                    RETURN_DATE_BEFORE_FIXED_DATE,
                    ACTUAL_RETURN_DATE
            )).multiply(LATE_FEE_MULTIPLIER).setScale(2, RoundingMode.HALF_UP));

    public static final String SESSION = "session ";
    public static final String EXISTING_SESSION_ID = SESSION + EXISTING_PAYMENT_WITH_ID_101;
    public static final String NOT_EXISTING_SESSION_ID = SESSION + NOT_EXISTING_PAYMENT_ID;
    public static final String EXISTING_SESSION_URL = "https://checkout.stripe.com/pay/session_test_id";
    public static final String SUCCESS_URL = "http://localhost/success";
    public static final String CANCEL_URL = "http://localhost/cancel";

    public static final String PAYLOAD_TEST = "{\"id\":\"TEST\"}";
    public static final String SIG_HEADER_TEST = "t=123,v1=TEST";
    public static final String ENDPOINT_SECRET = "whsec_test_secret";
    public static final String SESSION_STATUS_COMPLETE = "checkout.session.completed";

    public static final String UPDATED = "Updated ";
    public static final String UPDATED_STATUS = "SUV";
    public static final BigDecimal UPDATED_DAILY_FEE = new BigDecimal("100.0")
            .setScale(1, RoundingMode.HALF_UP);
    public static final String TEST_LONG_DATA = """
            TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST
            TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST
            TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST
            TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST
            TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST
            TESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTESTTEST
            """;
    public static final String INVALID_CAR_MODEL = TEST_LONG_DATA;
    public static final String INVALID_CAR_BRAND = "T";
    public static final String INVALID_CAR_TYPE = "E";
    public static final String NOT_EXISTING_CAR_TYPE = "NOT_EXISTING_TYPE";
    public static final int INVALID_CAR_INVENTORY = -1;
    public static final BigDecimal INVALID_CAR_DAILY_FEE = new BigDecimal("999.999");
    public static final BigDecimal INVALID_AMOUNT_TO_PAY = new BigDecimal("999.999");
    public static final int INVALID_MAX_RENTAL_DAYS = 999;
    public static final int INVALID_MIN_RENTAL_DAYS = 1;
    public static final String INVALID_SESSION_ID = "non-existent-session-id";
    public static final String INVALID_STRIPE_AMOUNT_TO_PAY = "non-existent-payment-id";
    public static final String INVALID_PAYMENT_TYPE = "UNKNOWN_TYPE";
    public static final String INVALID_PAYMENT_RENTAL_ID = "non-existent-rental-id";
    public static final String INVALID_SESSION_STATUS = "invalid-session-status";
    public static final String INVALID_SIG_HEADER_TEST = "invalid-sig-header";
    public static final String INVALID_PAYLOAD_TEST = "{\"id\":\"invalid\"}";

    public static final String[] CAR_IGNORING_FIELDS = new String[] {"id", "type", "isDeleted"};
    public static final String CAR_DTO_IGNORING_FIELD = "id";
    public static final String USER_DTO_IGNORING_FIELD = "id";
    public static final String[] RENTAL_DTO_IGNORING_FIELDS = new String[] {
            "id", "userId", "carDto.id"
    };
    public static final String[] PAYMENT_IGNORING_FIELDS = new String[] {"id", "rentalId"};
    public static final String[] NEW_PAYMENT_IGNORING_FIELDS = new String[] {
            "id",
            "rentalId",
            "sessionUrl",
            "sessionId"
    };
    public static final String SESSION_METADATA_IGNORING_FIELD = "rentalId";

    public static final Pageable CAR_PAGEABLE = PageRequest.of(
            0,
            10,
            Sort.by("dailyFee").ascending()
    );
    public static final Pageable RENTAL_PAGEABLE = PageRequest.of(
            0,
            10,
            Sort.by("returnDate").descending()
    );
    public static final Pageable PAYMENT_PAGEABLE = PageRequest.of(
            0,
            10,
            Sort.by("rentalId").descending()
    );

    protected TestDataUtil() {
    }

    public static CarDto createTestCarDto(Long id) {
        CarDto carDto = new CarDto();
        carDto.setId(id);
        carDto.setModel(CAR_MODEL + id);
        carDto.setBrand(CAR_BRAND + id);
        carDto.setType(CAR_TYPE);
        carDto.setInventory(CAR_INVENTORY);
        carDto.setDailyFee(BigDecimal.valueOf(id).setScale(1, RoundingMode.HALF_UP));
        return carDto;
    }

    public static CarDto createUpdatedTestCarDto(Long id) {
        CarDto carDto = createTestCarDto(id);
        carDto.setModel(UPDATED + carDto.getModel());
        carDto.setBrand(UPDATED + carDto.getBrand());
        carDto.setType(UPDATED_STATUS);
        carDto.setInventory(CAR_INVENTORY);
        carDto.setDailyFee(UPDATED_DAILY_FEE);
        return carDto;
    }

    public static Car createTestCar(CarDto carDto) {
        Car car = new Car();
        car.setId(carDto.getId());
        car.setModel(carDto.getModel());
        car.setBrand(carDto.getBrand());
        car.setType(Car.Type.valueOf(carDto.getType()));
        car.setInventory(carDto.getInventory());
        car.setDailyFee(carDto.getDailyFee());
        return car;
    }

    public static Car createTestCar(Long id) {
        Car car = new Car();
        car.setId(id);
        car.setModel(CAR_MODEL + id);
        car.setBrand(CAR_BRAND + id);
        car.setType(Car.Type.valueOf(CAR_TYPE));
        car.setInventory(CAR_INVENTORY);
        car.setDailyFee(BigDecimal.valueOf(id).setScale(1, RoundingMode.HALF_UP));
        return car;
    }

    public static CarRequestDto createTestCarRequestDto(CarDto carDto) {
        return new CarRequestDto(
                carDto.getModel(),
                carDto.getBrand(),
                carDto.getType(),
                carDto.getInventory(),
                carDto.getDailyFee()
        );
    }

    public static CarRequestDto createTestInvalidCarRequestDto() {
        return new CarRequestDto(
                INVALID_CAR_MODEL,
                INVALID_CAR_BRAND,
                INVALID_CAR_TYPE,
                INVALID_CAR_INVENTORY,
                INVALID_CAR_DAILY_FEE
        );
    }

    public static UpdateCarDto createTestUpdateCarDto(CarDto carDto) {
        return new UpdateCarDto(
                carDto.getModel(),
                carDto.getBrand(),
                carDto.getType(),
                carDto.getDailyFee()
        );
    }

    public static UpdateCarDto createTestInvalidUpdateCarDto() {
        return new UpdateCarDto(
                INVALID_CAR_MODEL,
                INVALID_CAR_BRAND,
                INVALID_CAR_TYPE,
                INVALID_CAR_DAILY_FEE
        );
    }

    public static InventoryRequestDto createTestInventoryRequestDto(int inventory,
                                                                    OperationType operation) {
        InventoryRequestDto dto = new InventoryRequestDto();
        dto.setInventory(inventory);
        dto.setOperation(operation);
        return dto;
    }

    public static List<CarDto> createTestCarDtoList(Long startId, int size) {
        return LongStream.range(startId, startId + size)
                .mapToObj(TestDataUtil::createTestCarDto)
                .toList();
    }

    public static UserResponseDto createTestUserResponseDto(Long id) {
        return new UserResponseDto(
                id,
                id + EMAIL_DOMAIN,
                FIRST_NAME,
                LAST_NAME,
                User.Role.CUSTOMER.toString()
        );
    }

    public static UserResponseDto createTestUserResponseDto(Long id, User.Role role) {
        return new UserResponseDto(
                id,
                id + EMAIL_DOMAIN,
                FIRST_NAME,
                LAST_NAME,
                role.toString()
        );
    }

    public static User createTestUser(UserResponseDto userDto) {
        User user = new User();
        user.setId(userDto.id());
        user.setEmail(userDto.email());
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setPassword(B_CRYPT_PASSWORD);
        user.setRole(User.Role.valueOf(userDto.role()));
        return user;
    }

    public static User createTestUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + EMAIL_DOMAIN);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setPassword(B_CRYPT_PASSWORD);
        return user;
    }

    public static UserRegistrationRequestDto createTestUserRegistrationRequestDto(
            UserResponseDto userDto) {
        return new UserRegistrationRequestDto(
                userDto.email(),
                NOT_HASHED_PASSWORD,
                NOT_HASHED_PASSWORD,
                userDto.firstName(),
                userDto.lastName()
        );
    }

    public static UserRegistrationRequestDto createTestUserRegistrationRequestDto(Long id) {
        return new UserRegistrationRequestDto(
                id + EMAIL_DOMAIN,
                NOT_HASHED_PASSWORD,
                NOT_HASHED_PASSWORD,
                FIRST_NAME,
                LAST_NAME
        );
    }

    public static UserLoginRequestDto createTestUserLoginRequestDto(Long id) {
        return new UserLoginRequestDto(
                id + EMAIL_DOMAIN,
                NOT_HASHED_PASSWORD
        );
    }

    public static UserProfileUpdateDto createTestUpdateUserDto(UserResponseDto userDto) {
        return new UserProfileUpdateDto(
                userDto.email(),
                userDto.firstName(),
                userDto.lastName()
        );
    }

    public static UserProfileUpdateDto createTestUpdateUserDto(Long id) {
        return new UserProfileUpdateDto(
                id + EMAIL_DOMAIN,
                FIRST_NAME,
                LAST_NAME
        );
    }

    public static CustomUserDetails createCustomUserDetailsDto(User user, User.Role role) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getAuthorities(),
                role,
                user.isEnabled()
        );
    }

    public static UserChangePasswordDto createTestChangePasswordRequestDto() {
        return new UserChangePasswordDto(
                NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD
        );
    }

    public static RentalResponseDto createTestRentalResponseDto(Long id,
                                                                LocalDate actualReturnDate) {
        RentalResponseDto dto = new RentalResponseDto();
        dto.setId(id);
        dto.setUserId(id);
        dto.setRentalDate(String.valueOf(RENTAL_DATE));
        dto.setReturnDate(String.valueOf(RETURN_DATE));
        dto.setBaseRentalCost(BigDecimal.valueOf(id * NUMBER_OF_RENTAL_DAYS)
                .setScale(1, RoundingMode.HALF_UP));
        dto.setCarDto(createTestCarDto(id));
        if (actualReturnDate != null) {
            dto.setActualReturnDate(String.valueOf(actualReturnDate));
            dto.setStatus(RentalStatus.RETURNED);
        } else {
            dto.setStatus(RentalStatus.ACTIVE);
        }
        return dto;
    }

    public static RentalResponseDto createNewTestRentalResponseDto(Long id) {
        RentalResponseDto dto = new RentalResponseDto();
        dto.setId(id);
        dto.setUserId(id);
        dto.setRentalDate(String.valueOf(FIXED_DATE));
        dto.setReturnDate(String.valueOf(LocalDate.parse(dto.getRentalDate())
                .plusDays(NUMBER_OF_RENTAL_DAYS)));
        dto.setBaseRentalCost(BigDecimal.valueOf(id * NUMBER_OF_RENTAL_DAYS)
                .setScale(1, RoundingMode.HALF_UP));
        dto.setCarDto(createTestCarDto(id));
        dto.setStatus(RentalStatus.ACTIVE);
        return dto;
    }

    public static RentalDetailedDto createTestRentalDetailedDto(Long id,
                                                                LocalDate actualReturnDate) {
        RentalDetailedDto dto = new RentalDetailedDto();
        dto.setId(id);
        dto.setUserId(id);
        dto.setRentalDate(String.valueOf(RENTAL_DATE));
        dto.setReturnDate(String.valueOf(RETURN_DATE));
        dto.setCarDto(createTestCarDto(id));

        long rentalDays = ChronoUnit.DAYS.between(
                LocalDate.parse(dto.getRentalDate()),
                LocalDate.parse(dto.getReturnDate())
        );
        dto.setBaseRentalCost(BigDecimal.valueOf(id * rentalDays)
                        .setScale(1, RoundingMode.HALF_UP));

        dto.setPenaltyAmount(BigDecimal.ZERO);
        dto.setTotalCost(dto.getBaseRentalCost().add(dto.getPenaltyAmount()));
        dto.setAmountPaid(BigDecimal.ZERO);
        dto.setAmountDue(dto.getTotalCost().subtract(dto.getAmountPaid()));
        if (actualReturnDate == null) {
            dto.setStatus(RentalStatus.ACTIVE);
        } else {
            dto.setStatus(RentalStatus.RETURNED);
            dto.setActualReturnDate(String.valueOf(actualReturnDate));
        }
        return dto;
    }

    public static RentalDetailedDto createTestRentalDetailedDtoWithPenalty(Long id) {
        RentalDetailedDto dto = new RentalDetailedDto();
        dto.setId(id);
        dto.setUserId(id);
        dto.setRentalDate(String.valueOf(RENTAL_DATE));
        dto.setReturnDate(String.valueOf(RETURN_DATE_BEFORE_FIXED_DATE));
        dto.setActualReturnDate(String.valueOf(FIXED_DATE));
        dto.setCarDto(createTestCarDto(id));
        dto.setStatus(RentalStatus.RETURNED);

        BigDecimal dailyFee = dto.getCarDto().getDailyFee();
        long rentalDays = ChronoUnit.DAYS.between(
                LocalDate.parse(dto.getRentalDate()),
                LocalDate.parse(dto.getReturnDate())
        );
        BigDecimal expectedBaseRental = dailyFee.multiply(BigDecimal.valueOf(rentalDays))
                        .setScale(1, RoundingMode.HALF_UP);
        dto.setBaseRentalCost(expectedBaseRental);
        dto.setAmountPaid(BigDecimal.ZERO);

        long daysLate = ChronoUnit.DAYS.between(
                LocalDate.parse(dto.getReturnDate()),
                LocalDate.parse(dto.getActualReturnDate())
        );
        BigDecimal penaltyAmount = dailyFee.multiply(BigDecimal.valueOf(daysLate))
                .multiply(LATE_FEE_MULTIPLIER);
        dto.setPenaltyAmount(penaltyAmount);

        BigDecimal expectedTotalCost = expectedBaseRental.add(penaltyAmount);
        dto.setTotalCost(expectedTotalCost);

        BigDecimal expectedAmountDue = expectedTotalCost.subtract(dto.getAmountPaid());
        dto.setAmountDue(expectedAmountDue);
        return dto;
    }

    public static Rental createTestRental(RentalResponseDto rentalResponseDto) {
        final String actualReturnDate = rentalResponseDto.getActualReturnDate();

        Rental rental = new Rental();
        rental.setId(rentalResponseDto.getId());
        rental.setUserId(rentalResponseDto.getUserId());
        rental.setCarId(rentalResponseDto.getCarDto().getId());
        rental.setRentalDate(LocalDate.parse(rentalResponseDto.getRentalDate()));
        rental.setReturnDate(LocalDate.parse(rentalResponseDto.getReturnDate()));
        if (actualReturnDate != null) {
            rental.setActualReturnDate(LocalDate.parse(actualReturnDate));
        }
        return rental;
    }

    public static Rental createTestRental(RentalDetailedDto rentalDetailedDto) {
        final String actualReturnDate = rentalDetailedDto.getActualReturnDate();

        Rental rental = new Rental();
        rental.setId(rentalDetailedDto.getId());
        rental.setUserId(rentalDetailedDto.getUserId());
        rental.setCarId(rentalDetailedDto.getCarDto().getId());
        rental.setRentalDate(LocalDate.parse(rentalDetailedDto.getRentalDate()));
        rental.setReturnDate(LocalDate.parse(rentalDetailedDto.getReturnDate()));
        if (actualReturnDate != null) {
            rental.setActualReturnDate(LocalDate.parse(actualReturnDate));
        }
        return rental;
    }

    public static Rental createTestRental(Long id, LocalDate actualReturnDate) {
        Rental rental = new Rental();
        rental.setId(id);
        rental.setUserId(id);
        rental.setCarId(id);
        rental.setRentalDate(RENTAL_DATE);
        rental.setReturnDate(RETURN_DATE);
        rental.setActualReturnDate(actualReturnDate);
        return rental;
    }

    public static RentalRequestDto createTestRentalRequestDto(RentalResponseDto rentalDto) {
        return new RentalRequestDto(
                LocalDate.parse(rentalDto.getReturnDate()),
                rentalDto.getCarDto().getId()
        );
    }

    public static PaymentResponseDto createTestPaymentResponseDto(Long id, Payment.Status status) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(id);
        dto.setRentalId(EXISTING_RENTAL_ID);
        dto.setSessionId(SESSION + id);
        dto.setSessionUrl(EXISTING_SESSION_URL);
        dto.setAmountToPay(AMOUNT_TO_PAY);
        dto.setType(Payment.Type.PAYMENT);
        dto.setStatus(status);
        return dto;
    }

    public static PaymentResponseDto createNewTestPaymentResponseDto(Long id,
                                                                     Payment.Status status) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(id);
        dto.setRentalId(EXISTING_RENTAL_ID);
        dto.setSessionId(SESSION + id);
        dto.setSessionUrl(EXISTING_SESSION_URL);
        dto.setAmountToPay(AMOUNT_TO_PAY_FOR_NEW_PAYMENT);
        dto.setType(Payment.Type.PAYMENT);
        dto.setStatus(status);
        return dto;
    }

    public static PaymentResponseDto createNewTestPaymentResponseDtoTypeFine(
            Long id,
            Payment.Status status
    ) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(id);
        dto.setRentalId(EXISTING_RENTAL_ID);
        dto.setSessionId(SESSION + id);
        dto.setSessionUrl(EXISTING_SESSION_URL);
        dto.setAmountToPay(FINE_AMOUNT_TO_PAY);
        dto.setType(Payment.Type.FINE);
        dto.setStatus(status);
        return dto;
    }

    public static Payment createTestPayment(PaymentResponseDto paymentResponseDto) {
        Payment payment = new Payment();
        payment.setId(paymentResponseDto.getId());
        payment.setRentalId(paymentResponseDto.getRentalId());
        payment.setSessionId(paymentResponseDto.getSessionId());
        payment.setSessionUrl(paymentResponseDto.getSessionUrl());
        payment.setAmountToPay(paymentResponseDto.getAmountToPay());
        payment.setType(paymentResponseDto.getType());
        payment.setStatus(paymentResponseDto.getStatus());
        return payment;
    }

    public static Payment createTestPayment(Long id, Payment.Status status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setRentalId(EXISTING_RENTAL_ID);
        payment.setSessionId(SESSION + id);
        payment.setSessionUrl(EXISTING_SESSION_URL);
        payment.setAmountToPay(AMOUNT_TO_PAY);
        payment.setType(Payment.Type.PAYMENT);
        payment.setStatus(status);
        return payment;
    }

    public static Payment createTestPaymentTypeFine(Long id, Payment.Status status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setRentalId(EXISTING_RENTAL_ID);
        payment.setSessionId(SESSION + id);
        payment.setSessionUrl(EXISTING_SESSION_URL);
        payment.setAmountToPay(FINE_AMOUNT_TO_PAY);
        payment.setType(Payment.Type.FINE);
        payment.setStatus(status);
        return payment;
    }

    public static StripeSessionMetadataDto createTestStripeSessionMetadataDto(
            PaymentResponseDto paymentResponseDto) {
        return new StripeSessionMetadataDto(
                paymentResponseDto.getSessionId(),
                paymentResponseDto.getRentalId(),
                paymentResponseDto.getType(),
                paymentResponseDto.getAmountToPay(),
                paymentResponseDto.getSessionUrl()
        );
    }

    public static StripeSessionMetadataDto createTestStripeSessionMetadataDto(
            Payment payment) {
        return new StripeSessionMetadataDto(
                payment.getSessionId(),
                payment.getRentalId(),
                payment.getType(),
                payment.getAmountToPay(),
                payment.getSessionUrl()
        );
    }

    public static StripeSessionMetadataDto createTestStripeSessionMetadataDto(
            Long id,
            BigDecimal amountToPay
    ) {
        return new StripeSessionMetadataDto(
                SESSION + id,
                id,
                Payment.Type.PAYMENT,
                amountToPay,
                EXISTING_SESSION_URL
        );
    }
}
