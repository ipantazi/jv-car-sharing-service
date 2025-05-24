package com.github.ipantazi.carsharing.util;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.user.UserLoginRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationResponseDto;
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
    public static final Long NEGATIVE_ID = -1L;
    public static final Long EXISTING_CAR_ID = 101L;
    public static final Long ALTERNATIVE_CAR_ID = 103L;
    public static final Long SAFE_DELETED_CAR_ID = 104L;
    public static final Long NEW_CAR_ID = 105L;
    public static final Long NOT_EXISTING_CAR_ID = 999L;
    public static final Long EXISTING_USER_ID = 101L;
    public static final Long ALTERNATIVE_USER_ID = 102L;
    public static final Long NEW_USER_ID = 103L;
    public static final Long NOT_EXISTING_USER_ID = 999L;

    public static final String CAR_MODEL = "Test Car ";
    public static final String CAR_BRAND = "Test Brand ";
    public static final String CAR_TYPE = "UNIVERSAL";
    public static final int CAR_INVENTORY = 5;
    public static final BigDecimal CAR_DAILY_FEE = new BigDecimal("100.0")
            .setScale(1, RoundingMode.HALF_UP);

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

    public static final String EMAIL_DOMAIN = "@example.com";
    public static final String PASSWORD = "Test&password1";
    public static final String B_CRYPT_PASSWORD = "$2a$10$TYVQIW25Boqejv0QvAYYn.6nQHmiypul1BkRgww"
            + "1wPxSuLYBUg0f.";
    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";

    public static final String CAR_DTO_IGNORING_ID = "id";
    public static final String[] CAR_DTO_IGNORING_FIELDS = new String[] {"id", "dailyFee"};
    public static final String USER_DTO_IGNORING_FIELD = "id";

    public static final Pageable CAR_PAGEABLE = PageRequest.of(
            0,
            10,
            Sort.by("dailyFee").ascending()
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

    public static UserRegistrationResponseDto createTestUserRegistrationResponseDto(Long id) {
        return new UserRegistrationResponseDto(
                id,
                id + EMAIL_DOMAIN,
                FIRST_NAME,
                LAST_NAME
        );
    }

    public static User createTestUser(UserRegistrationResponseDto userDto) {
        User user = new User();
        user.setId(userDto.id());
        user.setEmail(userDto.email());
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setPassword(B_CRYPT_PASSWORD);
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
            UserRegistrationResponseDto userDto) {
        return new UserRegistrationRequestDto(
                userDto.email(),
                PASSWORD,
                PASSWORD,
                userDto.firstName(),
                userDto.lastName()
        );
    }

    public static UserRegistrationRequestDto createTestUserRegistrationRequestDto(Long id) {
        return new UserRegistrationRequestDto(
                id + EMAIL_DOMAIN,
                PASSWORD,
                PASSWORD,
                FIRST_NAME,
                LAST_NAME
        );
    }

    public static UserLoginRequestDto createTestUserLoginRequestDto(Long id) {
        return new UserLoginRequestDto(
                id + EMAIL_DOMAIN,
                PASSWORD
        );
    }
}
