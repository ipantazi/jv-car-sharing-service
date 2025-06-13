package com.github.ipantazi.carsharing.repository.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:database/users/insert-test-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:database/users/clear-all-users.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
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
}
