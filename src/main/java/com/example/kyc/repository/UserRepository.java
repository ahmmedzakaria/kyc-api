package com.example.kyc.repository;

import com.example.kyc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByMobile(String mobile);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndOtpCode(String email, String otpCode);
}

