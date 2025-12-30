/*
package com.github.ipantazi.carsharing.repository.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.USER_DTO_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.model.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {
        "classpath:database/cars/insert-test-cars.sql",
        "classpath:database/users/insert-test-users.sql",
        "classpath:database/rentals/insert-test-rentals.sql"
},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {
        "classpath:database/rentals/clear-all-rentals.sql",
        "classpath:database/users/clear-all-users.sql",
        "classpath:database/cars/clear-all-cars.sql"
},
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Check for safe deleted user existence by id.")
    void existsSoftDeletedUserById_ExistingUser_ReturnsTrue() {
        // Given
        Long expected = 1L;

        // When
        Long actual = userRepository.existsSoftDeletedUserById(SAFE_DELETED_USER_ID);

        // Then
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    @DisplayName("Check for safe deleted user existence by id.")
    void existsSoftDeletedUserById_NotExistingSafeDeletedUser_ReturnsFalse() {
        // Given
        Long expected = 0L;

        // When
        Long actual = userRepository.existsSoftDeletedUserById(EXISTING_USER_ID);

        // Then
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    @DisplayName("Get email by rental id.")
    void getEmailByRentalId_ExistingRentalId_ReturnsEmail() {
        // When
        Optional<String> actual = userRepository.getEmailByRentalId(EXISTING_RENTAL_ID);

        // Then
        assertThat(actual).isPresent().hasValue(EXISTING_EMAIL);
    }

    @Test
    @DisplayName("Get email by rental id.")
    void getEmailByRentalId_NotExistingRentalId_ReturnsEmpty() {
        // When
        Optional<String> actual = userRepository.getEmailByRentalId(NOT_EXISTING_RENTAL_ID);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Test lockUserForUpdate() method with existing user ID.")
    void lockUserForUpdate_ExistingUser_ReturnsUser() {
        // Given
        User expectedUser = createTestUser(EXISTING_USER_ID);

        // When
        Optional<User> actualUserOpt = userRepository.lockUserForUpdate(EXISTING_USER_ID);

        // Then
        assertThat(actualUserOpt).isPresent();
        assertObjectsAreEqualIgnoringFields(
                actualUserOpt.get(),
                expectedUser,
                USER_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test lockUserForUpdate() method when user ID doesn't exist.")
    void lockUserForUpdate_NotExistingUser_ReturnsEmptyOptional() {
        // When
        Optional<User> actualUser = userRepository.lockUserForUpdate(NOT_EXISTING_USER_ID);

        // Then
        assertThat(actualUser).isEmpty();
    }
}

 */
