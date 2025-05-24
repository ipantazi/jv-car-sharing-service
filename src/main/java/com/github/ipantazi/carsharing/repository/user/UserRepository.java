package com.github.ipantazi.carsharing.repository.user;

import com.github.ipantazi.carsharing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
