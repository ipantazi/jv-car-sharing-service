package com.github.ipantazi.carsharing.concurrency;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIRST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.LAST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserRegistrationRequestDto;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.config.BaseConcurrencyIntegrationTest;
import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRoleUpdateDto;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import com.github.ipantazi.carsharing.service.user.UserService;
import com.github.ipantazi.carsharing.util.concurrency.ConcurrencyTestHelper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

public class ConcurrentUserProfileIntegrationTest extends BaseConcurrencyIntegrationTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    public static void beforeAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
        executeSqlScript(dataSource, "database/users/insert-test-users.sql");
    }

    @AfterEach
    public void afterEach(@Autowired DataSource dataSource) {
        executeSqlScript(dataSource,"database/users/restoring-users.sql");
    }

    @AfterAll
    public static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    public static void teardown(DataSource dataSource) {
        executeSqlScript(dataSource, "database/users/clear-all-users.sql");
    }

    @RepeatedTest(3)
    @DisplayName("Two threads updating the profile cannot overwrite each other — "
            + "final state must be a valid serialized update.")
    public void updateUserProfile_twoThreadsUpdatingTheProfile_CannotOverwriteEachOther()
            throws InterruptedException {
        // Given
        int threadCount = 2;

        UserProfileUpdateDto dtoA = new UserProfileUpdateDto(
                EXISTING_EMAIL,
                FIRST_NAME + "UpdatedA",
                LAST_NAME + "UpdatedA"
        );

        UserProfileUpdateDto dtoB = new UserProfileUpdateDto(
                EXISTING_EMAIL,
                FIRST_NAME + "UpdatedB",
                LAST_NAME + "UpdatedB"
        );

        List<Callable<String>> tasks = getTasksUserProfileUpdate(dtoA, dtoB);
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<String>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        String resultA = ConcurrencyTestHelper.safeGet(results.get(0));
        String resultB = ConcurrencyTestHelper.safeGet(results.get(1));

        assertThat(resultA).isEqualTo("UPDATED_A_OK");
        assertThat(resultB).isEqualTo("UPDATED_B_OK");

        User user = userRepository.findById(EXISTING_USER_ID).orElseThrow();
        assertThat(List.of(dtoA.firstName(), dtoB.firstName()))
                .as("There is exactly one of two first name updates (either A or B) in the db.")
                .contains(user.getFirstName());
        assertThat(List.of(dtoA.lastName(), dtoB.lastName()))
                .as("There is exactly one of two last name updates (either A or B) in the db.")
                .contains(user.getLastName());
    }

    @Test
    @DisplayName("Two users concurrently trying to change their email to the same value")
    public void updateUserProfile_twoThreadsUsingTheSameNewEmail_OnlyOneEmailChangeSucceeds()
            throws InterruptedException {
        // Given
        int threadCount = 2;

        UserProfileUpdateDto dto = new UserProfileUpdateDto(NEW_EMAIL, FIRST_NAME, LAST_NAME);

        List<Callable<Boolean>> tasks = getTasksUserPasswordUpdate(
                dto, List.of(EXISTING_USER_ID, EXISTING_ID_ANOTHER_USER));
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<Boolean>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        long successCount = results.stream()
                .map(ConcurrencyTestHelper::safeGet)
                .filter(success -> success)
                .count();
        assertThat(successCount)
                .as("Only one email change can succeed "
                        + "when two concurrent requests use the same new email.")
                .isEqualTo(1);

        User userAFromDb = userRepository.findById(EXISTING_USER_ID).orElseThrow();
        User userBFromDb = userRepository.findById(EXISTING_ID_ANOTHER_USER).orElseThrow();
        assertThat(NEW_EMAIL).isIn(userAFromDb.getEmail(),userBFromDb.getEmail());
    }

    @Test
    @DisplayName("Only one password change can succeed when two concurrent requests use the same "
            + "old password.")
    public void changePassword_twoThreadsUsingTheSameOldPassword_OnlyOnePasswordChangeSucceeds()
            throws InterruptedException {
        // Given
        int threadCount = 2;

        UserChangePasswordDto dtoA = new UserChangePasswordDto(
                NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD + "UpdatedA",
                NEW_NOT_HASHED_PASSWORD + "UpdatedA"
        );

        UserChangePasswordDto dtoB = new UserChangePasswordDto(
                NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD + "UpdatedB",
                NEW_NOT_HASHED_PASSWORD + "UpdatedB"
        );

        List<Callable<Boolean>> tasks = getTasksChangePassword(List.of(dtoA, dtoB));
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<Boolean>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        long successCount = results.stream()
                .map(ConcurrencyTestHelper::safeGet)
                .filter(v -> v)
                .count();
        assertThat(successCount)
                .as("Only one password change can succeed when two concurrent requests use "
                        + "the same old password.")
                .isEqualTo(1);

        User user = userRepository.findById(EXISTING_USER_ID).orElseThrow();
        assertThat(passwordEncoder.matches(
                NEW_NOT_HASHED_PASSWORD + "UpdatedA", user.getPassword())
                || passwordEncoder.matches(
                NEW_NOT_HASHED_PASSWORD + "UpdatedB", user.getPassword())
        ).isTrue();

    }

    @Test
    @DisplayName("Two administrators cannot perform conflicting role updates at the same time "
            + "without serialization")
    public void updateUserRole_twoThreadsUpdatingTheUserRole_CannotOverwriteEachOther()
            throws InterruptedException {
        // Given
        int threadCount = 2;

        UserRoleUpdateDto dtoManager = new UserRoleUpdateDto(User.Role.MANAGER.toString());
        UserRoleUpdateDto dtoCustomer = new UserRoleUpdateDto(User.Role.CUSTOMER.toString());

        List<Callable<String>> tasks = getTasksChangeRole(dtoManager, dtoCustomer);
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<String>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        String resultManager = ConcurrencyTestHelper.safeGet(results.get(0));
        String resultCustomer = ConcurrencyTestHelper.safeGet(results.get(1));

        assertThat(resultManager).isEqualTo("ROLE_UPDATED_MANAGER_OK");
        assertThat(resultCustomer).isEqualTo("ROLE_UPDATED_CUSTOMER_OK");
    }

    @Test
    @DisplayName("Only one user should be created when 5 threads register the same email.")
    @Sql(scripts = "classpath:database/users/remove-new-test-user-from-user-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void register_5ThreadsRegisteringTheSameEmail_OnlyOneUserIsCreated()
            throws InterruptedException {
        // Given
        int threadCount = 5;
        long initialCount = userRepository.count();
        UserRegistrationRequestDto registrationRequestDto = createTestUserRegistrationRequestDto(
                NEW_USER_ID
        );

        List<Callable<Boolean>> tasks = getTasksRegisterUser(threadCount, registrationRequestDto);

        // When
        List<Future<Boolean>> results = new ConcurrencyTestHelper(threadCount)
                .runConcurrentTasks(tasks);

        // Then
        long successCount = results.stream()
                .map(ConcurrencyTestHelper::safeGet)
                .filter(v -> v)
                .count();
        assertThat(successCount)
                .as("Only one user should be created when 5 threads register the same email.")
                .isEqualTo(1);

        assertThat(userRepository.count())
                .as("Only one user must be stored in DB")
                .isEqualTo(initialCount + 1);
    }

    private List<Callable<String>> getTasksUserProfileUpdate(UserProfileUpdateDto dtoA,
                                                           UserProfileUpdateDto dtoB) {
        return List.of(
                () -> {
                    try {
                        userService.updateUserProfile(EXISTING_USER_ID, dtoA);
                        return "UPDATED_A_OK";
                    } catch (Exception e) {
                        return "UPDATED_A_ERROR";
                    }
                },
                () -> {
                    try {
                        userService.updateUserProfile(EXISTING_USER_ID, dtoB);
                        return "UPDATED_B_OK";
                    } catch (Exception e) {
                        return "UPDATED_B_ERROR";
                    }
                }
        );
    }

    private List<Callable<Boolean>> getTasksUserPasswordUpdate(UserProfileUpdateDto dto,
                                                           List<Long> userIds) {
        return userIds.stream()
                .map(userId -> (Callable<Boolean>) () -> {
                    try {
                        userService.updateUserProfile(userId, dto);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    private List<Callable<Boolean>> getTasksChangePassword(List<UserChangePasswordDto> dtos) {
        return dtos.stream()
                .map(dto -> (Callable<Boolean>) () -> {
                    try {
                        userService.changePassword(EXISTING_USER_ID, dto);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    private List<Callable<String>> getTasksChangeRole(UserRoleUpdateDto dtoManager,
                                                       UserRoleUpdateDto dtoCustomer) {
        return List.of(
                () -> {
                    try {
                        userService.updateUserRole(EXISTING_USER_ID, dtoManager);
                        return "ROLE_UPDATED_MANAGER_OK";
                    } catch (Exception e) {
                        return "ROLE_UPDATED_MANAGER_ERROR";
                    }
                },
                () -> {
                    try {
                        userService.updateUserRole(EXISTING_USER_ID, dtoCustomer);
                        return "ROLE_UPDATED_CUSTOMER_OK";
                    } catch (Exception e) {
                        return "ROLE_UPDATED_CUSTOMER_ERROR";
                    }
                }
        );
    }

    private List<Callable<Boolean>> getTasksRegisterUser(int threadCount,
                                                         UserRegistrationRequestDto dto) {
        return IntStream.range(0, threadCount)
                .mapToObj(i -> (Callable<Boolean>) () -> {
                    try {
                        userService.register(dto);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }
}
