package com.github.ipantazi.carsharing.controller.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.ACTUAL_RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_INVENTORY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXPECTED_RENTALS_SIZE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_INSTANT;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEGATIVE_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NUMBER_OF_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DTO_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ZONE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createNewTestRentalResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalDetailedDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalDetailedDtoWithPenalty;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertCollectionsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationError;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationErrorList;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.BAD_REQUEST;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.FORBIDDEN;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.NOT_FOUND;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.UNAUTHORIZED;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_RENTALS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_RENTAL_BY_ID;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_RETURN_RENTAL;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.createRequestWithPageable;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parsePageContent;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parseResponseToObject;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.toJson;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createJsonMvcResult;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createMvcResult;
import static com.github.ipantazi.carsharing.util.controller.SecurityTestUtil.authenticateTestUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ipantazi.carsharing.config.BaseIntegrationTest;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.model.User;
import java.time.Clock;
import java.util.List;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(RentalControllerTest.FixedClockTestConfig.class)
public class RentalControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
     void beforeAll() {
        teardown();
        executeSqlScript(
                dataSource,
                "database/users/insert-test-users.sql",
                "database/cars/insert-test-cars.sql",
                "database/rentals/insert-test-rentals.sql"
        );
    }

    @AfterAll
    void afterAll() {
        teardown();
    }

    @SneakyThrows
    void teardown() {
        executeSqlScript(
                dataSource,
                "database/rentals/clear-all-rentals.sql",
                "database/cars/clear-all-cars.sql",
                "database/users/clear-all-users.sql"
        );
    }

    @TestConfiguration
    static class FixedClockTestConfig {
        @Bean
        @Primary
        public Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZONE);
        }
    }

    @Test
    @DisplayName("Test create rental with valid request and authorized user with CUSTOMER role")
    @Sql(scripts = {
            "classpath:database/rentals/remove-new-test-rental-from-rentals-table.sql",
            "classpath:database/cars/restoring-car-id101.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createRental_ValidRequest_Success() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalResponseDto expectedRentalResponseDto = createNewTestRentalResponseDto(
                NEW_RENTAL_ID, FIXED_DATE);
        expectedRentalResponseDto.setUserId(EXISTING_USER_ID);
        int expectedCarInventory = CAR_INVENTORY - 1;
        expectedRentalResponseDto.getCarDto().setInventory(expectedCarInventory);
        RentalRequestDto rentalRequestDto = createTestRentalRequestDto(expectedRentalResponseDto);
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isCreated(),
                jsonRequest
        );

        // Then
        RentalResponseDto actualRentalResponseDto = parseResponseToObject(
                result,
                objectMapper,
                RentalResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualRentalResponseDto,
                expectedRentalResponseDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to create a rental")
    void createRental_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(NUMBER_OF_RENTAL_DAYS),
                EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return Forbidden when non-CUSTOMER tries to create a rental")
    void createRental_NonCustomer_ShouldReturnForbidden() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(NUMBER_OF_RENTAL_DAYS),
                EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isForbidden(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("Test create rental with exceeding min rental period.")
    void createRental_ExceedingMinRentalPeriod_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(INVALID_MIN_RENTAL_DAYS),
                EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Return date must be no earlier than %d day in the future"
                        .formatted(MIN_RENTAL_DAYS)
        );
    }

    @Test
    @DisplayName("Test create rental with exceeding max rental period.")
    void createRental_ExceedingMaxRentalPeriod_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(INVALID_MAX_RENTAL_DAYS),
                EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Maximum rental period is %d days.".formatted(MAX_RENTAL_DAYS)
        );
    }

    @Test
    @DisplayName("Test create rental when no cars available for rent.")
    @Sql(scripts = "classpath:database/cars/set-inventory-null-for-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/restoring-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createRental_NoCarsAvailable_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(NUMBER_OF_RENTAL_DAYS),
                EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Not enough cars with ID: %d".formatted(rentalRequestDto.carId())
        );
    }

    @Test
    @DisplayName("Test create rental when car not found.")
    void createRental_NonExistsCarId_ShouldReturnNotFound() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(NUMBER_OF_RENTAL_DAYS),
                NOT_EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isNotFound(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Car not found with id: %d"
                        .formatted(rentalRequestDto.carId())
        );
    }

    @Test
    @DisplayName("Test create rental when car id is negative.")
    void createRental_NegativeCarId_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(NUMBER_OF_RENTAL_DAYS),
                NEGATIVE_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'carId': Car ID must be a positive number")
        );
    }

    @Test
    @DisplayName("Test create rental when car id and return date is null.")
    void createRental_NullCarIdAndReturnDate_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                null,
                null
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'carId': Car ID cannot be null",
                        "Field 'returnDate': Return date cannot be null")
        );
    }

    @Test
    @DisplayName("Test create rental when return date is in the past.")
    void createRental_ReturnDateInThePast_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.minusDays(INVALID_MAX_RENTAL_DAYS),
                EXISTING_CAR_ID
        );
        String jsonRequest = toJson(objectMapper, rentalRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_RENTALS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Return date must be in the future"
        );
    }

    @Test
    @DisplayName("Test get rentals with isActive true and authorized user with CUSTOMER role")
    void getRentals_IsActiveTrueAndCustomerRole_ShouldReturnRentalResponseDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        Boolean isActive = true;
        RentalResponseDto responseDto = createTestRentalResponseDto(
                EXISTING_USER_ID, null);
        List<RentalResponseDto> expectedResponseDtoList = List.of(responseDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                        .param("is_active", String.valueOf(isActive))
                        .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualResponseDtoList,
                expectedResponseDtoList,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                RENTAL_PAGEABLE,
                expectedResponseDtoList.size()
        );
        RentalResponseDto actualDto = actualResponseDtoList.get(0);
        assertThat(actualDto.getActualReturnDate()).isNull();
        assertThat(actualDto.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    @DisplayName("Test get rentals with isActive false and authorized user with CUSTOMER role")
    @Sql(scripts = "classpath:database/rentals/set-actual-rental-date-for-rental-id101.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/rentals/restoring-rental-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getRentals_IsActiveFalseAndCustomerRole_ShouldReturnRentalResponseDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        Boolean isActive = false;
        RentalResponseDto responseDto = createTestRentalResponseDto(
                EXISTING_USER_ID, ACTUAL_RETURN_DATE);
        List<RentalResponseDto> expectedResponseDtoList = List.of(responseDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", String.valueOf(isActive))
                                .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualResponseDtoList,
                expectedResponseDtoList,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                RENTAL_PAGEABLE,
                expectedResponseDtoList.size()
        );
        RentalResponseDto actualDto = actualResponseDtoList.get(0);
        assertThat(actualDto.getActualReturnDate()).isEqualTo(ACTUAL_RETURN_DATE.toString());
        assertThat(actualDto.getStatus()).isEqualTo(RentalStatus.RETURNED);
    }

    @Test
    @DisplayName("Test get rentals when the user specified someone else's ID")
    void getRentals_PathVariableUserIdIsAlien_ShouldIgnoringVariableAndReturnRentalResponseDto()
            throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        Boolean isActive = true;
        RentalResponseDto responseDto = createTestRentalResponseDto(
                EXISTING_USER_ID, null);
        List<RentalResponseDto> expectedResponseDtoList = List.of(responseDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", String.valueOf(isActive))
                                .param("user_id", String.valueOf(EXISTING_ID_ANOTHER_USER)),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualResponseDtoList,
                expectedResponseDtoList,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                RENTAL_PAGEABLE,
                expectedResponseDtoList.size()
        );
        RentalResponseDto actualDto = actualResponseDtoList.get(0);
        assertThat(actualDto.getActualReturnDate()).isNull();
        assertThat(actualDto.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to get rentals")
    void getRentals_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        Boolean isActive = true;

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", String.valueOf(isActive))
                                .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isUnauthorized()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test get rentals when isActive and userId are null")
    void getRentals_IsActiveAndUserIdNull_ShouldReturnRentalResponseDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertThat(actualResponseDtoList).isNotNull();
        assertThat(actualResponseDtoList).isNotEmpty();
        assertThat(actualResponseDtoList).hasSize(EXPECTED_RENTALS_SIZE);
    }

    @Test
    @DisplayName("Test get rentals with the MANAGER role and the CUSTOMER userId.")
    void getRentals_ManagerRoleAndCustomerId_ShouldReturnRentalResponseDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        Boolean isActive = true;
        RentalResponseDto responseDto = createTestRentalResponseDto(
                EXISTING_USER_ID, null);
        List<RentalResponseDto> expectedResponseDtoList = List.of(responseDto);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", String.valueOf(isActive))
                                .param("user_id", String.valueOf(EXISTING_USER_ID)),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertCollectionsAreEqualIgnoringFields(
                actualResponseDtoList,
                expectedResponseDtoList,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                RENTAL_PAGEABLE,
                expectedResponseDtoList.size()
        );
    }

    @Test
    @DisplayName("Test get rentals with the MANAGER role and non existing userId.")
    void getRentals_ManagerRoleAndNonExistingUserId_ShouldReturnNotFound() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        Boolean isActive = true;

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", String.valueOf(isActive))
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
    @DisplayName("Test get rentals with the MANAGER role and negative userId.")
    void getRentals_ManagerRoleAndNegativeUserId_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        Boolean isActive = true;

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", String.valueOf(isActive))
                                .param("user_id", String.valueOf(NEGATIVE_ID)),
                status().isBadRequest()
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'user_id': Invalid user id. User id should be greater than 0"));
    }

    @Test
    @DisplayName("Test get rentals when isActive and userId is not in valid format.")
    void getRentals_IsActiveAndUserIdIsNotInValidFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("is_active", "INVALID")
                                .param("user_id", "INVALID"),
                status().isBadRequest()
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of(
                        "Field 'user_id': Failed to convert value of type 'java.lang.String' to "
                                + "required type 'java.lang.Long'; For input string: \"INVALID\"",
                        "Field 'is_active': Failed to convert value of type 'java.lang.String' to "
                                + "required type 'java.lang.Boolean'; "
                                + "Invalid boolean value [INVALID]"
                ));
    }

    @Test
    @DisplayName("Test get rentals with invalid name path parameter.")
    void getRentals_InvalidNamePathParameter_ShouldIgnoringParameterAndReturnRentalResponseDto()
            throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE)
                                .param("INVALID", String.valueOf(true))
                                .param("INVALID", String.valueOf(EXISTING_USER_ID)),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertThat(actualResponseDtoList).isNotNull();
        assertThat(actualResponseDtoList).isNotEmpty();
        assertThat(actualResponseDtoList).hasSize(EXPECTED_RENTALS_SIZE);
    }

    @Test
    @DisplayName("Test get rentals when no rentals exist.")
    @Sql(scripts = "classpath:database/rentals/clear-all-rentals.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/rentals/insert-test-rentals.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getRentals_NoRentalsExist_ShouldReturnEmptyList() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_RENTALS, RENTAL_PAGEABLE),
                status().isOk()
        );

        // Then
        List<RentalResponseDto> actualResponseDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<RentalResponseDto>>() {
                }
        );
        assertThat(actualResponseDtoList).isNotNull();
        assertThat(actualResponseDtoList).isEmpty();
    }

    @Test
    @DisplayName("Test get rental by id with valid rental id and CUSTOMER role.")
    void getRentalById_ValidIdAndCustomerRole_ShouldReturnRentalDetailedDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalDetailedDto expectedDto = createTestRentalDetailedDto(EXISTING_USER_ID, null);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_RENTAL_BY_ID, EXISTING_RENTAL_ID),
                status().isOk()
        );

        // Then
        RentalDetailedDto actualDto = parseResponseToObject(
                result,
                objectMapper,
                RentalDetailedDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualDto,
                expectedDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
    }

    @Test
    @DisplayName("Test get rental by id when MANAGER find rental by any customer.")
    void getRentalById_GetRentalAnyCustomer_ReturnsRentalDetailedDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        RentalDetailedDto expectedDto = createTestRentalDetailedDto(EXISTING_USER_ID, null);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_RENTAL_BY_ID, EXISTING_RENTAL_ID),
                status().isOk()
        );

        // Then
        RentalDetailedDto actualDto = parseResponseToObject(
                result,
                objectMapper,
                RentalDetailedDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualDto,
                expectedDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to get a rental by id")
    void getRentalById_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_RENTAL_BY_ID, EXISTING_RENTAL_ID),
                status().isUnauthorized()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test get rental by id with invalid rental id.")
    void getRentalById_InvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_RENTAL_BY_ID, NOT_EXISTING_RENTAL_ID),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Rental not found with id: " + NOT_EXISTING_RENTAL_ID
        );
    }

    @Test
    @DisplayName("Test get rental by id when CUSTOMER is looking for alien rental.")
    void getRentalById_AlienRental_ThrowsException() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_RENTAL_BY_ID, EXISTING_RENTAL_ID_ANOTHER_USER),
                status().isForbidden()
        );
        // Then
        assertValidationError(
                result,
                objectMapper,
                FORBIDDEN,
                "Access denied. You do not have permission to perform this action."
        );
    }

    @Test
    @DisplayName("Test return rental with valid rental id of any customer and MANAGER role.")
    @Sql(scripts = {
            "classpath:database/rentals/restoring-rental-id101.sql",
            "classpath:database/cars/restoring-car-id101.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void returnRental_ValidIdAndManagerRole_Success() throws Exception {
        // Given
        authenticateTestUser(EXISTING_RENTAL_ID_ANOTHER_USER, User.Role.MANAGER);

        int expectedInventory = CAR_INVENTORY + 1;
        RentalDetailedDto expectedDto = createTestRentalDetailedDto(EXISTING_USER_ID, FIXED_DATE);
        expectedDto.getCarDto().setInventory(expectedInventory);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, EXISTING_RENTAL_ID),
                status().isOk()
        );

        // Then
        RentalDetailedDto actualDto = parseResponseToObject(
                result,
                objectMapper,
                RentalDetailedDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualDto,
                expectedDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertThat(actualDto.getActualReturnDate()).isEqualTo(FIXED_DATE.toString());
        assertThat(actualDto.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(actualDto.getCarDto().getInventory()).isEqualTo(expectedInventory);
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to return a rental")
    void returnRental_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, EXISTING_RENTAL_ID),
                status().isUnauthorized()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test return rental with valid rental id and CUSTOMER role.")
    @Sql(scripts = {
            "classpath:database/rentals/restoring-rental-id101.sql",
            "classpath:database/cars/restoring-car-id101.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void returnRental_ValidIdAndCustomerRole_Success() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        int expectedInventory = CAR_INVENTORY + 1;
        RentalDetailedDto expectedDto = createTestRentalDetailedDto(EXISTING_USER_ID, FIXED_DATE);
        expectedDto.getCarDto().setInventory(expectedInventory);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, EXISTING_RENTAL_ID),
                status().isOk()
        );

        // Then
        RentalDetailedDto actualDto = parseResponseToObject(
                result,
                objectMapper,
                RentalDetailedDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualDto,
                expectedDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertThat(actualDto.getActualReturnDate()).isEqualTo(FIXED_DATE.toString());
        assertThat(actualDto.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(actualDto.getCarDto().getInventory()).isEqualTo(expectedInventory);
    }

    @Test
    @DisplayName("Test return rental with penalty amount and CUSTOMER role and when actual return "
            + "date greater than expected return date.")
    @Sql(scripts = "classpath:database/rentals/set-return-date-for-rental-id101.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/restoring-rental-id101.sql",
            "classpath:database/cars/restoring-car-id101.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void returnRental_WithPenaltyAndCustomerRole_ReturnRentalDetailedDto() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        RentalDetailedDto expectedDto = createTestRentalDetailedDtoWithPenalty(EXISTING_USER_ID);

        int expectedInventory = CAR_INVENTORY + 1;
        expectedDto.getCarDto().setInventory(expectedInventory);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, EXISTING_RENTAL_ID),
                status().isOk()
        );

        // Then
        RentalDetailedDto actualDto = parseResponseToObject(
                result,
                objectMapper,
                RentalDetailedDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualDto,
                expectedDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
    }

    @Test
    @DisplayName("Test return rental with CUSTOMER role when the rental has already been returned")
    @Sql(scripts = "classpath:database/rentals/set-actual-rental-date-for-rental-id101.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/restoring-rental-id101.sql",
            "classpath:database/cars/restoring-car-id101.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void returnRental_ActualReturnDate_ShouldReturnBadRequest() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, EXISTING_RENTAL_ID),
                status().isBadRequest()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Rental with id %d is already returned on %s"
                        .formatted(EXISTING_RENTAL_ID, ACTUAL_RETURN_DATE)
        );
    }

    @Test
    @DisplayName("Test return rental with CUSTOMER role when rental id does not exist")
    void returnRental_InvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, NOT_EXISTING_RENTAL_ID),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Rental not found with id: " + NOT_EXISTING_RENTAL_ID
        );
    }

    @Test
    @DisplayName("Test return rental with CUSTOMER role when rental is linked to another client.")
    void returnRental_AlienRental_ShouldReturnNotFound() throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                post(URL_RETURN_RENTAL, EXISTING_RENTAL_ID_ANOTHER_USER),
                status().isForbidden()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                FORBIDDEN,
                "Access denied. You do not have permission to perform this action."
        );
    }
}
