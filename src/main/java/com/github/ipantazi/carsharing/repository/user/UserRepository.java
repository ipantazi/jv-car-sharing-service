package com.github.ipantazi.carsharing.repository.user;

import com.github.ipantazi.carsharing.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<UserDetails> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = """
    SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
    FROM users
    WHERE id = ? AND is_deleted = TRUE                                        
            """, nativeQuery = true)
    Long existsSoftDeletedUserById(@Param("id") Long id);
}
