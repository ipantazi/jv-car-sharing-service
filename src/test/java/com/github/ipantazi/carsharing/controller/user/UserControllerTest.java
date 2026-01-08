package com.github.ipantazi.carsharing.controller.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EMAIL_DOMAIN;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIRST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.LAST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TEST_LONG_DATA;
import static com.github.ipantazi.carsharing.util.TestDataUtil.USER_DTO_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestChangePasswordRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUpdateUserDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationError;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertValidationErrorList;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.BAD_REQUEST;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.CONFLICT;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_CHANGE_PASSWORD_FORMAT_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_CHANGE_PASSWORD_NULL_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_UPDATE_PROFILE_FORMAT_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.EXPECTED_UPDATE_PROFILE_NULL_ERRORS;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.FORBIDDEN;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.NOT_FOUND;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.UNAUTHORIZED;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_USER_ME;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_USER_ROLE;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.parseResponseToObject;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestUtil.toJson;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createJsonMvcResult;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createMvcResult;
import static com.github.ipantazi.carsharing.util.controller.SecurityTestUtil.authenticateTestUser;
import static com.github.ipantazi.carsharing.util.controller.SecurityTestUtil.setAuthenticationForUser;
import static com.github.ipantazi.carsharing.util.controller.SecurityTestUtil.wrapAsUserDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ipantazi.carsharing.config.BaseIntegrationTest;
import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserRoleUpdateDto;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import com.github.ipantazi.carsharing.security.CustomUserDetails;
import java.util.List;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class UserControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    void beforeAll() {
        teardown();
        executeSqlScript(dataSource, "database/users/insert-test-users.sql");
    }

    @AfterAll
     void afterAll() {
        teardown();
    }

    @SneakyThrows
    void teardown() {
        executeSqlScript(dataSource, "database/users/clear-all-users.sql");
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Test update user role with valid request")
    @Sql(
            scripts = "classpath:database/users/restoring-users.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateUserRole_ValidRequest_Success() throws Exception {
        //Given
        String expectedRole = User.Role.MANAGER.toString();
        UserResponseDto expectedUserresponseDto = new UserResponseDto(
                EXISTING_USER_ID,
                EXISTING_USER_ID + EMAIL_DOMAIN,
                FIRST_NAME,
                LAST_NAME,
                expectedRole
        );
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(expectedRole);
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, EXISTING_USER_ID),
                status().isOk(),
                jsonRequest
        );

        //Then
        UserResponseDto actualUserResponseDto = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserresponseDto,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Should return Unauthorized when unauthorized user tries to update user role")
    void updateUserRole_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        //Given
        String expectedRole = User.Role.MANAGER.toString();
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(expectedRole);
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, EXISTING_USER_ID),
                status().isUnauthorized(),
                jsonRequest
        );

        //Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @WithMockUser(username = "alice@example.com")
    @Test
    @DisplayName("Should return Forbidden when non-MANAGER tries to update user role")
    void updateUserRole_NonManagerUser_ShouldReturnForbidden() throws Exception {
        //Given
        String expectedRole = User.Role.MANAGER.toString();
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(expectedRole);
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, EXISTING_USER_ID),
                status().isForbidden(),
                jsonRequest
        );

        //Then
        assertThat(result.getResponse().getStatus()).isEqualTo(FORBIDDEN);
    }

    @WithMockUser(username = "bob.example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Should return Not Found when trying to update role for non-existing user ID")
    void updateUserRole_InvalidUserId_ShouldReturnNotFound() throws Exception {
        //Given
        String expectedRole = User.Role.MANAGER.toString();
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(expectedRole);
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, NOT_EXISTING_USER_ID),
                status().isNotFound(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                NOT_FOUND,
                "User not found with id: " + NOT_EXISTING_USER_ID
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Should return Bad Request when trying to update role with role null")
    void updateUserRole_NullRole_ShouldReturnBadRequest() throws Exception {
        //Given
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(null);
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, EXISTING_USER_ID),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'role': Invalid role. Role can't be blank.")
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Should return Bad Request when trying to update role with invalid role size")
    void updateUserRole_InvalidRoleSize_ShouldReturnBadRequest() throws Exception {
        //Given
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(TEST_LONG_DATA);
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, EXISTING_USER_ID),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'role': Invalid role. Role must be between 5 and 20 characters.")
        );
    }

    @WithMockUser(username = "bob@example.com", roles = {"MANAGER"})
    @Test
    @DisplayName("Should return Bad Request when trying to update role with invalid role")
    void updateUserRole_InvalidRole_ShouldReturnBadRequest() throws Exception {
        //Given
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto("INVALID_ROLE");
        String jsonRequest = toJson(objectMapper, userRoleUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ROLE, EXISTING_USER_ID),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "No enum constant com.github.ipantazi.carsharing.model.User.Role.INVALID_ROLE"
        );
    }

    @Test
    @DisplayName("Test get user details with valid user ID and authorized user with MANAGER role")
    void getUserDetails_ValidUserId_AuthorizedManager_ShouldReturnUserResponseDto()
            throws Exception {
        // Given
        UserResponseDto expectedUserResponseDto = createTestUserResponseDto(
                EXISTING_ID_ANOTHER_USER,
                User.Role.MANAGER
        );
        User user = createTestUser(expectedUserResponseDto);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_USER_ME),
                status().isOk()
        );

        //Then
        UserResponseDto actualUserResponseDto = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test get user details with valid user ID and authorized user with CUSTOMER role")
    void getUserDetails_ValidUserId_AuthorizedCustomer_ShouldReturnUserResponseDto()
            throws Exception {
        // Given
        UserResponseDto expectedUserResponseDto = createTestUserResponseDto(
                EXISTING_USER_ID,
                User.Role.CUSTOMER);
        User user = createTestUser(expectedUserResponseDto);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_USER_ME),
                status().isOk()
        );

        // Then
        UserResponseDto actualUserResponseDto = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test get user details with unauthorized user")
    void getUserDetails_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // When
        MvcResult result = createMvcResult(
                mockMvc,
                get(URL_USER_ME),
                status().isUnauthorized()
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test update user profile with role CUSTOMER")
    @Sql(
            scripts = "classpath:database/users/restoring-users.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateUserProfile_ValidRequestWithRoleCustomer_shouldReturnUserResponseDto()
            throws Exception {
        // Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        UserResponseDto expectedUserResponseDto = new UserResponseDto(
                EXISTING_USER_ID,
                NEW_EMAIL,
                "New" + FIRST_NAME,
                "New" + LAST_NAME,
                User.Role.CUSTOMER.toString()
        );
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(
                expectedUserResponseDto);
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isOk(),
                jsonRequest
        );

        // Then
        UserResponseDto actualUserResponseDto = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test update user profile with role MANAGER")
    @Sql(
            scripts = "classpath:database/users/restoring-users.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateUserProfile_ValidRequestWithRoleManager_shouldReturnUserResponseDto()
            throws Exception {
        // Given
        authenticateTestUser(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);

        UserResponseDto expectedUserResponseDto = new UserResponseDto(
                EXISTING_ID_ANOTHER_USER,
                NEW_EMAIL,
                "New" + FIRST_NAME,
                "New" + LAST_NAME,
                User.Role.MANAGER.toString()
        );
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(
                expectedUserResponseDto);
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isOk(),
                jsonRequest
        );

        // Then
        UserResponseDto actualUserResponseDto = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test update user profile with unauthorized user")
    void updateUserProfile_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(EXISTING_USER_ID);
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isUnauthorized(),
                jsonRequest
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test update user profile when e-mail is unchanged")
    @Sql(
            scripts = "classpath:database/users/restoring-users.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void updateUserProfile_UnchangedEmail_ShouldReturnUserResponseDto() throws Exception {
        //Given
        User user = createTestUser(EXISTING_USER_ID);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        String currentEmail = user.getEmail();
        UserResponseDto expectedUserResponseDto = new UserResponseDto(
                EXISTING_USER_ID,
                currentEmail,
                "New" + FIRST_NAME,
                "New" + LAST_NAME,
                User.Role.CUSTOMER.toString()
        );
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(
                expectedUserResponseDto);
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        // When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isOk(),
                jsonRequest
        );

        // Then
        UserResponseDto actualUserResponseDto = parseResponseToObject(
                result,
                objectMapper,
                UserResponseDto.class
        );
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test update user profile when email already in use")
    void updateUserProfile_EmailAlreadyInUse_ShouldReturnConflict() throws Exception {
        //Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        String newEmail = EXISTING_ID_ANOTHER_USER + EMAIL_DOMAIN;
        UserProfileUpdateDto userProfileUpdateDto = new UserProfileUpdateDto(
                newEmail,
                "New" + FIRST_NAME,
                "New" + LAST_NAME
        );
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isConflict(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                CONFLICT,
                "Email already in use: " + newEmail
        );
    }

    @Test
    @DisplayName("Test update user profile when request data is null")
    void updateUserProfile_RequestDataIsNull_ShouldReturnBadRequest() throws Exception {
        //Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        UserProfileUpdateDto userProfileUpdateDto = new UserProfileUpdateDto(
                null,
                null,
                null
        );
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_UPDATE_PROFILE_NULL_ERRORS
        );
    }

    @Test
    @DisplayName("Test update user profile with invalid request data format")
    void updateUserProfile_InvalidRequestDataFormat_ShouldReturnBadRequest() throws Exception {
        //Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        UserProfileUpdateDto userProfileUpdateDto = new UserProfileUpdateDto(
                TEST_LONG_DATA,
                TEST_LONG_DATA,
                TEST_LONG_DATA
        );
        String jsonRequest = toJson(objectMapper, userProfileUpdateDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                put(URL_USER_ME),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_UPDATE_PROFILE_FORMAT_ERRORS
        );
    }

    @Test
    @DisplayName("Test change password with role CUSTOMER")
    @Sql(
            scripts = "classpath:database/users/restoring-users.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void changePassword_CustomerRole_Success() throws Exception {
        //Given
        User user = createTestUser(EXISTING_USER_ID);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        UserChangePasswordDto userChangePasswordDto = createTestChangePasswordRequestDto();
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);
        String oldPasswordHash = user.getPassword();

        //When
        createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isNoContent(),
                jsonRequest
        );

        //Then
        User updatedUser = userRepository.findById(EXISTING_USER_ID).orElseThrow();
        assertThat(oldPasswordHash).isNotEqualTo(updatedUser.getPassword());
        assertThat(passwordEncoder.matches(
                userChangePasswordDto.newPassword(),
                updatedUser.getPassword()
        )).isTrue();
    }

    @Test
    @DisplayName("Test change password with role MANAGER")
    @Sql(
            scripts = "classpath:database/users/restoring-users.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void changePassword_ManagerRole_Success() throws Exception {
        //Given
        User user = createTestUser(EXISTING_ID_ANOTHER_USER);
        user.setRole(User.Role.MANAGER);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        UserChangePasswordDto userChangePasswordDto = createTestChangePasswordRequestDto();
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);
        String oldPasswordHash = user.getPassword();

        //When
        createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isNoContent(),
                jsonRequest
        );

        //Then
        User updatedUser = userRepository.findById(EXISTING_ID_ANOTHER_USER).orElseThrow();
        assertThat(oldPasswordHash).isNotEqualTo(updatedUser.getPassword());
        assertThat(passwordEncoder.matches(
                userChangePasswordDto.newPassword(),
                updatedUser.getPassword()
        )).isTrue();
    }

    @Test
    @DisplayName("Test change password with unauthorized user")
    void changePassword_UnauthorizedUser_ShouldReturnUnauthorized() throws Exception {
        //Given
        UserChangePasswordDto userChangePasswordDto = createTestChangePasswordRequestDto();
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isUnauthorized(),
                jsonRequest
        );

        //Then
        assertThat(result.getResponse().getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test change password with null password")
    void changePassword_NullPassword_ShouldReturnBadRequest() throws Exception {
        //Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        UserChangePasswordDto userChangePasswordDto = new UserChangePasswordDto(
                null,
                null,
                null
        );
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_CHANGE_PASSWORD_NULL_ERRORS
        );
    }

    @Test
    @DisplayName("Test change password with invalid format password")
    void changePassword_InvalidFormatPassword_ShouldReturnBadRequest() throws Exception {
        //Given
        User user = createTestUser(EXISTING_USER_ID);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        UserChangePasswordDto userChangePasswordDto = new UserChangePasswordDto(
                user.getPassword(),
                TEST_LONG_DATA,
                TEST_LONG_DATA
        );
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                EXPECTED_CHANGE_PASSWORD_FORMAT_ERRORS
        );
    }

    @Test
    @DisplayName("Test change password with invalid old password")
    void changePassword_InvalidOldPassword_ShouldReturnBadRequest() throws Exception {
        //Given
        authenticateTestUser(EXISTING_USER_ID, User.Role.CUSTOMER);

        UserChangePasswordDto userChangePasswordDto = new UserChangePasswordDto(
                NOT_EXISTING_NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD
        );
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationError(
                result,
                objectMapper,
                BAD_REQUEST,
                "Old password is incorrect"
        );
    }

    @Test
    @DisplayName("Test change password when do not match new passwords")
    void changePassword_NewPasswordsNotMatch_ShouldReturnBadRequest() throws Exception {
        //Given
        User user = createTestUser(EXISTING_USER_ID);
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);

        UserChangePasswordDto userChangePasswordDto = new UserChangePasswordDto(
                user.getPassword(),
                NEW_NOT_HASHED_PASSWORD,
                ""
        );
        String jsonRequest = toJson(objectMapper, userChangePasswordDto);

        //When
        MvcResult result = createJsonMvcResult(
                mockMvc,
                patch(URL_USER_ME),
                status().isBadRequest(),
                jsonRequest
        );

        //Then
        assertValidationErrorList(
                result,
                objectMapper,
                List.of("Field 'newPassword': The passwords do not match.")
        );
    }
}
