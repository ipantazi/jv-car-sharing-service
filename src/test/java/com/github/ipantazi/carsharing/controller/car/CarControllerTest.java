package com.github.ipantazi.carsharing.controller.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_BRAND;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_DAILY_FEE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_DTO_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_INVENTORY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_MODEL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXPECTED_CARS_SIZE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_CAR_INVENTORY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_TYPE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCarDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCarDtoList;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCarRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestInvalidCarRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestInvalidUpdateCarDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestInventoryRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUpdateCarDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createUpdatedTestCarDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertCollectionsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationError;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationErrorList;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.BAD_REQUEST;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.CONFLICT;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_SAVE_CAR_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_SAVE_CAR_NULL_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_UPDATE_CAR_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_UPDATE_CAR_NULL_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_UPDATE_INVENTORY_CAR_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.FORBIDDEN;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.NOT_FOUND;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.NO_CONTENT;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.UNAUTHORIZED;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_CARS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_CARS_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_CARS_NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_CARS_SAFE_DELETED_CAR_ID;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.createRequestWithPageable;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parsePageContent;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parseResponseToObject;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static com.github.ipantazi.carsharing.util.controller.MockMvcUtil.buildMockMvc;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createJsonMvcResult;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createMvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import java.util.List;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CarControllerTest {
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(@Autowired DataSource dataSource,
                          @Autowired WebApplicationContext applicationContext) {
        mockMvc = buildMockMvc(applicationContext);
        teardown(dataSource);
        executeSqlScript(dataSource, "database/cars/insert-test-cars.sql");
    }

    @AfterAll
    static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        executeSqlScript(dataSource, "database/cars/clear-all-cars.sql");
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test createCar with valid request")
    @Sql(
            scripts = "classpath:database/cars/remove-new-test-car-from-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void createCar_ValidRequest_Success() throws Exception {
        // Given
        CarDto expectedCarDto = createTestCarDto(NEW_CAR_ID);
        CarRequestDto carRequestDto = createTestCarRequestDto(expectedCarDto);
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isCreated(),
                jsonRequest
        );

        // Then
        CarDto actualCarDto = parseResponseToObject(result, objectMapper, CarDto.class);
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to create a car")
    void createCar_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        CarRequestDto carRequestDto = createTestCarRequestDto(createTestCarDto(NEW_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @WithMockUser(username = "alice@example.com")
    @Test
    @DisplayName("Should return Forbidden when non-MANAGER tries to create a car")
    void createCar_NonManager_ShouldReturnForbidden() throws Exception {
        // Given
        CarRequestDto carRequestDto = createTestCarRequestDto(createTestCarDto(NEW_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isForbidden(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Verify that an exception is thrown when a new car is already present")
    void createCar_CarAlreadyPresent_ShouldReturnConflict() throws Exception {
        // Given
        CarRequestDto carRequestDto = createTestCarRequestDto(createTestCarDto(EXISTING_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Car with model: " + carRequestDto.model() + " and brand: " + carRequestDto.brand()
                        + " already exists."
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Verify that an exception is thrown when a new car is already present "
            + "but soft deleted")
    void createCar_CarAlreadyPresentButSoftDeleted_ShouldReturnConflict() throws Exception {
        // Given
        CarRequestDto carRequestDto = createTestCarRequestDto(
                createTestCarDto(SAFE_DELETED_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Car with model: " + carRequestDto.model() + " and brand: " + carRequestDto.brand()
                        + " already exists, but was previously deleted."
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Verify that an exception is thrown when car fields are not in valid format")
    void createCar_InvalidFormatCarFields_ShouldReturnBadRequest() throws Exception {
        // Given
        CarRequestDto carRequestDto = createTestInvalidCarRequestDto();
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(result, objectMapper, EXPECTED_SAVE_CAR_ERRORS);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Verify that an exception is thrown when a car fields are null")
    void createCar_NullCarFields_ShouldReturnBadRequest() throws Exception {
        // Given
        CarRequestDto carRequestDto = new CarRequestDto(
                null,
                null,
                null,
                0,
                null
        );
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(result, objectMapper, EXPECTED_SAVE_CAR_NULL_ERRORS);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Verify that an exception is thrown when a type is not valid")
    void createCar_InvalidType_ShouldReturnBadRequest() throws Exception {
        // Given
        CarRequestDto carRequestDto = new CarRequestDto(
                CAR_MODEL,
                CAR_BRAND,
                NOT_EXISTING_CAR_TYPE,
                CAR_INVENTORY,
                CAR_DAILY_FEE
        );
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_CARS),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Invalid car type: " + carRequestDto.type()
        );
    }

    @Test
    @DisplayName("Test getAll cars")
    void getAll_GivenCarsCatalog_ShouldReturnAllCars() throws Exception {
        // Given
        List<CarDto> expectedCarDtoList = createTestCarDtoList(EXISTING_CAR_ID, EXPECTED_CARS_SIZE);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_CARS, CAR_PAGEABLE),
                status().isOk()
        );

        // Then
        List<CarDto> actualCarDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<CarDto>>() {}
        );
        assertCollectionsAreEqualIgnoringFields(
                actualCarDtoList,
                expectedCarDtoList,
                CAR_DTO_IGNORING_FIELD
        );
        assertPageMetadataEquals(
                result,
                objectMapper,
                CAR_PAGEABLE,
                expectedCarDtoList.size());
    }

    @Test
    @DisplayName("Test getAll cars with empty catalog")
    @Sql(
            scripts = "classpath:database/cars/clear-all-cars.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:database/cars/insert-test-cars.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void getAll_GivenEmptyCarsCatalog_ShouldReturnEmptyPage() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                createRequestWithPageable(URL_CARS, CAR_PAGEABLE),
                status().isOk()
        );

        // Then
        List<CarDto> actualCarDtoList = parsePageContent(
                result,
                objectMapper,
                new TypeReference<List<CarDto>>() {}
        );
        assertThat(actualCarDtoList).isEmpty();
        assertPageMetadataEquals(
                result,
                objectMapper,
                CAR_PAGEABLE,
                0
        );
    }

    @Test
    @DisplayName("Test get car by id")
    void getCarById_ValidId_ShouldReturnCarDto() throws Exception {
        // Given
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_CARS_EXISTING_CAR_ID),
                status().isOk()
        );

        // Then
        CarDto actualCarDto = parseResponseToObject(result, objectMapper, CarDto.class);
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
    }

    @Test
    @DisplayName("Test get car by id with invalid id")
    void getCarById_InvalidId_ShouldReturnNotFound() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_CARS_NOT_EXISTING_CAR_ID),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Car not found with id: " + NOT_EXISTING_CAR_ID
        );
    }

    @Test
    @DisplayName("Test get car by id with soft deleted car")
    void getCarById_SoftDeletedCar_ShouldReturnNotFound() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_CARS_SAFE_DELETED_CAR_ID),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Car not found with id: " + SAFE_DELETED_CAR_ID
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test safe delete car by id")
    @Sql(
            scripts = "classpath:database/cars/restoring-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void deleteCar_ValidId_ShouldSafeDeletedCar() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                delete(URL_CARS_EXISTING_CAR_ID),
                status().isNoContent()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(NO_CONTENT);
        MvcResult getResult = createMvcResult(
                mockMvc,
                get(URL_CARS_EXISTING_CAR_ID),
                status().isNotFound()
        );
        assertThat(getResult.getResponse().getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to delete a car")
    void deleteCar_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                delete(URL_CARS_EXISTING_CAR_ID),
                status().isUnauthorized()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @WithMockUser(username = "alice@example.com")
    @Test
    @DisplayName("Should return Forbidden when non-MANAGER tries to delete a car")
    void deleteCar_NonManager_ShouldReturnForbidden() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                delete(URL_CARS_EXISTING_CAR_ID),
                status().isForbidden()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test safe delete car by id with invalid id")
    void deleteCar_InvalidId_ShouldReturnNotFound() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                delete(URL_CARS_NOT_EXISTING_CAR_ID),
                status().isNotFound()
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Car not found with id: " + NOT_EXISTING_CAR_ID
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update car by id")
    @Sql(
            scripts = "classpath:database/cars/restoring-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateCar_ValidRequest_Success() throws Exception {
        // Given
        CarDto expectedCarDto = createUpdatedTestCarDto(EXISTING_CAR_ID);
        UpdateCarDto updateCarDto = createTestUpdateCarDto(expectedCarDto);
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isOk(),
                jsonRequest
        );

        // Then
        CarDto actualCarDto = parseResponseToObject(result, objectMapper, CarDto.class);
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to update a car")
    void updateCar_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        UpdateCarDto updateCarDto = createTestUpdateCarDto(createTestCarDto(EXISTING_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @WithMockUser(username = "alice@example.com")
    @Test
    @DisplayName("Should return Forbidden when non-MANAGER tries to update a car")
    void updateCar_NonManager_ShouldReturnForbidden() throws Exception {
        // Given
        UpdateCarDto updateCarDto = createTestUpdateCarDto(createTestCarDto(EXISTING_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isForbidden(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update car by id with invalid id")
    void updateCar_InvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        UpdateCarDto updateCarDto = createTestUpdateCarDto(createTestCarDto(NOT_EXISTING_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_NOT_EXISTING_CAR_ID),
                status().isNotFound(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "Car not found with id: " + NOT_EXISTING_CAR_ID
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update car by id with an existing car with the given model and brand ")
    void updateCar_ExistingCarWithSameModelAndBrand_ShouldReturnConflict() throws Exception {
        // Given
        UpdateCarDto updateCarDto = createTestUpdateCarDto(
                createTestCarDto(EXISTING_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Car with model: " + updateCarDto.model() + " and brand: " + updateCarDto.brand()
                        + " already exists."
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName(
            "Test update car with an existing soft deleted car with the given model and brand"
    )
    void updateCar_ExistingSoftDeletedCarWithSameModelAndBrand_ShouldReturnConflict()
            throws Exception {
        // Given
        UpdateCarDto updateCarDto = createTestUpdateCarDto(
                createTestCarDto(SAFE_DELETED_CAR_ID));
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isConflict(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Car with model: " + updateCarDto.model() + " and brand: " + updateCarDto.brand()
                        + " already exists, but was previously deleted."
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update car by id with invalid car fields")
    void updateCar_InvalidCarFields_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateCarDto updateCarDto = createTestInvalidUpdateCarDto();
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(result, objectMapper, EXPECTED_UPDATE_CAR_ERRORS);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update car by id with null car fields")
    void updateCar_NullCarFields_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateCarDto updateCarDto = new UpdateCarDto(
                null,
                null,
                null,
                null
        );
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(result, objectMapper, EXPECTED_UPDATE_CAR_NULL_ERRORS);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update car by id with invalid type")
    void updateCar_InvalidType_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateCarDto updateCarDto = new UpdateCarDto(
                CAR_MODEL,
                CAR_BRAND,
                NOT_EXISTING_CAR_TYPE,
                CAR_DAILY_FEE
        );
        String jsonRequest = objectMapper.writeValueAsString(updateCarDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_CARS_EXISTING_CAR_ID),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "No enum constant com.github.ipantazi.carsharing.model.Car.Type."
                        + updateCarDto.type()
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update inventory by id with operation type SET")
    @Sql(
            scripts = "classpath:database/cars/restoring-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateInventory_ValidRequest_SetInventory() throws Exception {
        // Given
        int newInventory = 3;
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                newInventory,
                OperationType.SET
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isOk(),
                jsonRequest
        );

        // Then
        CarDto actualCarDto = parseResponseToObject(result, objectMapper, CarDto.class);
        assertThat(actualCarDto.getInventory()).isEqualTo(newInventory);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update inventory by id with operation type INCREASE")
    @Sql(
            scripts = "classpath:database/cars/restoring-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateInventory_ValidRequest_IncreaseInventory() throws Exception {
        // Given
        int newInventory = 3;
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                newInventory,
                OperationType.INCREASE
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isOk(),
                jsonRequest
        );

        // Then
        CarDto actualCarDto = parseResponseToObject(result, objectMapper, CarDto.class);
        assertThat(actualCarDto.getInventory()).isEqualTo(CAR_INVENTORY + newInventory);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update inventory by id with operation type DECREASE")
    @Sql(
            scripts = "classpath:database/cars/restoring-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateInventory_ValidRequest_DecreaseInventory() throws Exception {
        // Given
        int newInventory = 3;
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                newInventory,
                OperationType.DECREASE
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isOk(),
                jsonRequest
        );

        // Then
        CarDto actualCarDto = parseResponseToObject(result, objectMapper, CarDto.class);
        assertThat(actualCarDto.getInventory()).isEqualTo(CAR_INVENTORY - newInventory);
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to update inventory")
    void updateInventory_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                CAR_INVENTORY,
                OperationType.SET
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @WithMockUser(username = "alice@example.com")
    @Test
    @DisplayName("Should return Forbidden when non-MANAGER tries to update inventory")
    void updateInventory_NonManager_ShouldReturnForbidden() throws Exception {
        // Given
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                CAR_INVENTORY,
                OperationType.SET
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isForbidden(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update inventory by id with operation type DECREASE "
            + "and not enough inventory")
    void updateInventory_NotEnoughInventoryToDecrease_ShouldReturnBadRequest()
            throws Exception {
        // Given
        int newInventory = CAR_INVENTORY + 1;
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                newInventory,
                OperationType.DECREASE
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Not enough cars with ID: %d".formatted(EXISTING_CAR_ID)
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update inventory by id with invalid operation type")
    void updateInventory_InvalidOperationType_ShouldReturnBadRequest() throws Exception {
        // Given
        String jsonRequest = """
        {
          "inventory": 5,
          "operation": "NOT_EXISTING_TYPE"
        }
                """;

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'operation': Invalid operation. Operation shouldn't be null.")
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update inventory by id with invalid fields")
    void updateInventory_InvalidFields_ShouldReturnBadRequest() throws Exception {
        // Given
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                INVALID_CAR_INVENTORY,
                null
        );
        String jsonRequest = objectMapper.writeValueAsString(inventoryRequestDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_CARS_EXISTING_CAR_ID),
                status().isBadRequest(),
                jsonRequest
        );

        // Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_UPDATE_INVENTORY_CAR_ERRORS
        );
    }
}
