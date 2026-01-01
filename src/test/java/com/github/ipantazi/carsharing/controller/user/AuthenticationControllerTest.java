package com.github.ipantazi.carsharing.controller.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EMAIL_DOMAIN;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIRST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.LAST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TEST_LONG_DATA;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserLoginRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserRegistrationRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationError;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationErrorList;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.CONFLICT;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_LOGIN_BLANK_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_LOGIN_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_REGISTRATION_FORMAT_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_REGISTRATION_SIZE_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.UNAUTHORIZED;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_LOGIN;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_REGISTRATION;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parseResponseToObject;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static com.github.ipantazi.carsharing.util.controller.MockMvcUtil.buildMockMvc;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createJsonMvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ipantazi.carsharing.dto.user.UserLoginRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserLoginResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
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
public class AuthenticationControllerTest {
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(@Autowired DataSource dataSource,
                          @Autowired WebApplicationContext webApplicationContext) {
        mockMvc = buildMockMvc(webApplicationContext);
        teardown(dataSource);
        executeSqlScript(dataSource, "database/users/insert-test-users.sql");
    }

    @AfterAll
    static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        executeSqlScript(dataSource, "database/users/clear-all-users.sql");
    }

    @Test
    @DisplayName("Login of an existing user.")
    void login_ValidUserLoginRequestDto_Success() throws Exception {
        //Given
        UserLoginRequestDto userLoginRequestDto = createTestUserLoginRequestDto(EXISTING_USER_ID);
        String jsonRequest = objectMapper.writeValueAsString(userLoginRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_LOGIN),
                status().isOk(),
                jsonRequest
        );

        //Then
        UserLoginResponseDto actual = parseResponseToObject(
                result,
                objectMapper,
                UserLoginResponseDto.class
        );
        assertThat(actual.token()).isNotBlank();
        String[] tokenParts = actual.token().split("\\.");
        assertThat(tokenParts).hasSize(3);
    }

    @Test
    @DisplayName("Verify that an exception is trow when an email doesn't exists.")
    void login_NotExistingEmail_ShouldReturnUnauthorized() throws Exception {
        //Given
        UserLoginRequestDto userLoginRequestDto =
                createTestUserLoginRequestDto(NOT_EXISTING_USER_ID);
        String jsonRequest = objectMapper.writeValueAsString(userLoginRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_LOGIN),
                status().isUnauthorized(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                UNAUTHORIZED,
                "Email or password invalid"
        );
    }

    @Test
    @DisplayName("Verify that an exception is trow when the request fields are blank.")
    void login_BlankRequestFields_ShouldReturnBadRequest() throws Exception {
        //Given
        UserLoginRequestDto userLoginRequestDto = new UserLoginRequestDto("", "");
        String jsonRequest = objectMapper.writeValueAsString(userLoginRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_LOGIN),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_LOGIN_BLANK_ERRORS
        );
    }

    @Test
    @DisplayName("Verify that an exception is trow when the request fields format are not valid.")
    void login_InvalidFormatRequestFields_ShouldReturnBadRequest() throws Exception {
        //Given
        UserLoginRequestDto userLoginRequestDto = new UserLoginRequestDto(
                TEST_LONG_DATA,
                TEST_LONG_DATA
        );
        String jsonRequest = objectMapper.writeValueAsString(userLoginRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_LOGIN),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_LOGIN_ERRORS
        );
    }

    @Test
    @DisplayName("Verify that an exception is throw when the password is not valid.")
    void login_InvalidPassword_ShouldReturnUnauthorized() throws Exception {
        //Given
        UserLoginRequestDto userLoginRequestDto = new UserLoginRequestDto(
                EXISTING_USER_ID + EMAIL_DOMAIN,
                "invalid password"
        );
        String jsonRequest = objectMapper.writeValueAsString(userLoginRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_LOGIN),
                status().isUnauthorized(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                UNAUTHORIZED,
                "Email or password invalid"
        );
    }

    @Test
    @DisplayName("Verify that an exception is throw when the email is not valid.")
    void login_InvalidEmail_ShouldReturnUnauthorized() throws Exception {
        //Given
        UserLoginRequestDto userLoginRequestDto = new UserLoginRequestDto(
                "invalid" + EMAIL_DOMAIN,
                NOT_HASHED_PASSWORD
        );
        String jsonRequest = objectMapper.writeValueAsString(userLoginRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_LOGIN),
                status().isUnauthorized(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                UNAUTHORIZED,
                "Email or password invalid"
        );
    }

    @Test
    @Sql(scripts = "classpath:database/users/clear-all-users.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/users/clear-all-users.sql",
            "classpath:database/users/insert-test-users.sql",
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Registration of a new user.")
    void registerUser_ValidUserRegistrationRequestDto_Success() throws Exception {
        //Given
        UserResponseDto expected = createTestUserResponseDto(NEW_USER_ID);
        UserRegistrationRequestDto userRegistrationRequestDto =
                createTestUserRegistrationRequestDto(expected);
        String jsonRequest = objectMapper.writeValueAsString(userRegistrationRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_REGISTRATION),
                status().isOk(),
                jsonRequest
        );

        //Then
        UserResponseDto actual = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertThat(actual.email()).isEqualTo(expected.email());
    }

    @Test
    @DisplayName("Verify that an exception is trow when an email already exists.")
    void registerUser_EmailAlreadyExists_ShouldReturnBadRequest() throws Exception {
        //Given
        UserRegistrationRequestDto userRegistrationRequestDto =
                createTestUserRegistrationRequestDto(EXISTING_USER_ID);
        String jsonRequest = objectMapper.writeValueAsString(userRegistrationRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_REGISTRATION),
                status().isConflict(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Can't register user with this email: " + userRegistrationRequestDto.email()
        );
    }

    @Test
    @DisplayName("""
            Verify that an exception is trow when the size of the request fields is incorrect.
            """)
    void registerUser_IncorrectSizeOfRequestFields_ShouldReturnBadRequest() throws Exception {
        //Given
        UserRegistrationRequestDto userRegistrationRequestDto =
                new UserRegistrationRequestDto(
                        TEST_LONG_DATA + EMAIL_DOMAIN,
                        "",
                        "",
                        "",
                        ""
                );
        String jsonRequest = objectMapper.writeValueAsString(userRegistrationRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_REGISTRATION),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_REGISTRATION_SIZE_ERRORS
        );
    }

    @Test
    @DisplayName("""
            Verify that an exception is trow when the format of the request fields is invalid.
            """)
    void registerUser_InvalidFormatOfRequestFields_ShouldReturnBadRequest() throws Exception {
        //Given
        String invalidFormatOfValue = "INVALID FORMAT";
        UserRegistrationRequestDto userRegistrationRequestDto =
                new UserRegistrationRequestDto(
                        invalidFormatOfValue,
                        invalidFormatOfValue,
                        invalidFormatOfValue,
                        invalidFormatOfValue,
                        invalidFormatOfValue
                );
        String jsonRequest = objectMapper.writeValueAsString(userRegistrationRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_REGISTRATION),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_REGISTRATION_FORMAT_ERRORS
        );
    }

    @Test
    @DisplayName("Verify that an exception is trow when the passwords do not match.")
    void registerUser_PasswordsDoNotMatch_ShouldReturnBadRequest() throws Exception {
        //Given
        UserRegistrationRequestDto userRegistrationRequestDto =
                new UserRegistrationRequestDto(
                        NEW_USER_ID + EMAIL_DOMAIN,
                        NOT_HASHED_PASSWORD,
                        "",
                        FIRST_NAME,
                        LAST_NAME
                );
        String jsonRequest = objectMapper.writeValueAsString(userRegistrationRequestDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                post(URL_REGISTRATION),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'password': The passwords do not match.")
        );
    }
}
