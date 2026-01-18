package com.github.ipantazi.carsharing.repository.user;

import com.github.ipantazi.carsharing.model.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<UserDetails> findByEmail(String email);

    @Query(value = """
    SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
    FROM users
    WHERE id = ? AND is_deleted = TRUE
            """, nativeQuery = true)
    Long existsSoftDeletedUserById(@Param("id") Long id);

    @Query("""
    SELECT u.email
    FROM User u
    WHERE u.id IN (
        SELECT r.userId
        FROM Rental r
        WHERE r.id = :rentalId
    )
            """)
    Optional<String> getEmailByRentalId(@Param("rentalId") Long rentalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> lockUserForUpdate(Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
