package com.ainovel.app.user.repo;

import com.ainovel.app.user.model.EmailVerificationCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {
    Page<EmailVerificationCode> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(String email, String purpose, Instant after);

    Optional<EmailVerificationCode> findFirstByEmailIgnoreCaseAndPurposeAndCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email,
            String purpose,
            String code,
            Instant now
    );
}
