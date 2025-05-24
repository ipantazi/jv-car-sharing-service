package com.github.ipantazi.carsharing.repository.user;

import com.github.ipantazi.carsharing.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<UserDetails> findByEmail(String email);

    boolean existsByEmail(String email);
}
