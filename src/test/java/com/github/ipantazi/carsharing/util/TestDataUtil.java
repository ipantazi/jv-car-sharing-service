package com.github.ipantazi.carsharing.util;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserLoginRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    /*public static final Long EXISTING_RENTAL_ID = 101L;
    public static final Long EXISTING_RENTAL_ID_ANOTHER_USER = 102L;
    public static final Long NEW_RENTAL_ID = 103L;
    public static final Long NOT_EXISTING_RENTAL_ID = 999L;*/

    public static final String CAR_MODEL = "Test Car ";
    public static final String CAR_BRAND = "Test Brand ";
    public static final String CAR_TYPE = "UNIVERSAL";
    public static final int CAR_INVENTORY = 5;
    public static final BigDecimal CAR_DAILY_FEE = new BigDecimal("100.0")
            .setScale(1, RoundingMode.HALF_UP);

    public static final String EMAIL_DOMAIN = "@example.com";
    public static final String NEW_EMAIL = NEW_USER_ID + EMAIL_DOMAIN;
    public static final String NOT_HASHED_PASSWORD = "Test&password1";
    public static final String NOT_EXISTING_NOT_HASHED_PASSWORD = "Not&existingPassword1";
    public static final String NEW_NOT_HASHED_PASSWORD = "New&password1";
    public static final String B_CRYPT_PASSWORD = "$2a$10$TYVQIW25Boqejv0QvAYYn.6nQHmiypul1BkRgww"
            + "1wPxSuLYBUg0f.";
    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";

    /*public static final int MIN_RENTAL_DAYS = 2;
    public static final int MAX_RENTAL_DAYS = 30;
    public static final int NUMBER_OF_RENTAL_DAYS = 5;
    public static final BigDecimal LATE_FEE_MULTIPLIER = BigDecimal.valueOf(1.5);
    public static final LocalDate RENTAL_DATE = LocalDate.of(2025, 1, 1);
    public static final LocalDate ACTUAL_RETURN_DATE = RENTAL_DATE.plusDays(3);
    public static final LocalDate RETURN_DATE_LESS_THEN_ACTUAL = RENTAL_DATE.plusDays(2);
    public static final LocalDate ACTUAL_RETURN_DATE_GREATER_THEN_RETURN_DATE = RENTAL_DATE
            .plusDays(5);
    public static final ZoneId ZONE = ZoneId.of("UTC");
    public static final Instant FIXED_INSTANT = RENTAL_DATE.plusDays(4).atStartOfDay(ZONE)
            .toInstant();
    public static final LocalDate FIXED_DATE = LocalDate.ofInstant(FIXED_INSTANT, ZONE);*/

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
    /*public static final int INVALID_MAX_RENTAL_DAYS = 999;
    public static final int INVALID_MIN_RENTAL_DAYS = 1;*/

    public static final String[] CAR_IGNORING_FIELDS = new String[] {"id", "type", "isDeleted"};
    public static final String CAR_DTO_IGNORING_FIELD = "id";
    public static final String USER_DTO_IGNORING_FIELD = "id";
    /*public static final String[] RENTAL_DTO_IGNORING_FIELDS = new String[] {
            "id", "userId", "carDto.id"
    };*/

    public static final Pageable CAR_PAGEABLE = PageRequest.of(
            0,
            10,
            Sort.by("dailyFee").ascending()
    );
    /*public static final Pageable RENTAL_PAGEABLE = PageRequest.of(
            0,
            10,
            Sort.by("returnDate").descending()
    );*/

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

    public static UserChangePasswordDto createTestChangePasswordRequestDto() {
        return new UserChangePasswordDto(
                NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD
        );
    }

    /*public static RentalResponseDto createTestRentalResponseDto(Long id,
                                                                LocalDate actualReturnDate) {
        RentalResponseDto dto = new RentalResponseDto();
        dto.setId(id);
        dto.setUserId(id);
        dto.setRentalDate(String.valueOf(RENTAL_DATE));
        dto.setReturnDate(String.valueOf(LocalDate.parse(dto.getRentalDate())
                .plusDays(NUMBER_OF_RENTAL_DAYS)));
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
        dto.setReturnDate(String.valueOf(LocalDate.parse(dto.getRentalDate())
                .plusDays(NUMBER_OF_RENTAL_DAYS)));
        dto.setCarDto(createTestCarDto(id));
        dto.setBaseRentalCost(BigDecimal.valueOf(id * NUMBER_OF_RENTAL_DAYS)
                .setScale(1, RoundingMode.HALF_UP));
        dto.setPenaltyAmount(BigDecimal.ZERO);
        dto.setTotalCost(dto.getBaseRentalCost().add(dto.getPenaltyAmount()));
        dto.setAmountPaid(dto.getBaseRentalCost());
        dto.setAmountDue(dto.getTotalCost().subtract(dto.getAmountPaid()));
        if (actualReturnDate == null) {
            dto.setStatus(RentalStatus.ACTIVE);
        } else {
            dto.setStatus(RentalStatus.RETURNED);
            dto.setActualReturnDate(String.valueOf(actualReturnDate));
        }
        return dto;
    }

    public static RentalDetailedDto createTestRentalDetailedDtoWithPenalty(
            Long id,
            LocalDate returnDate,
            LocalDate actualReturnDate
    ) {
        RentalDetailedDto dto = new RentalDetailedDto();
        dto.setId(id);
        dto.setUserId(id);
        dto.setRentalDate(String.valueOf(RENTAL_DATE));
        dto.setReturnDate(returnDate.toString());
        dto.setActualReturnDate(actualReturnDate.toString());
        dto.setCarDto(createTestCarDto(id));
        dto.setStatus(RentalStatus.RETURNED);

        BigDecimal dailyFee = dto.getCarDto().getDailyFee();
        long daysForBaseRental = ChronoUnit.DAYS.between(RENTAL_DATE, returnDate);
        BigDecimal expectedBaseRental = dailyFee.multiply(BigDecimal.valueOf(daysForBaseRental));
        dto.setBaseRentalCost(expectedBaseRental);
        dto.setAmountPaid(expectedBaseRental);

        long daysLate = ChronoUnit.DAYS.between(returnDate, actualReturnDate);
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

    public static RentalRequestDto createTestRentalRequestDto(RentalResponseDto rentalDto) {
        return new RentalRequestDto(
                LocalDate.parse(rentalDto.getReturnDate()),
                rentalDto.getCarDto().getId()
        );
    }*/
}
