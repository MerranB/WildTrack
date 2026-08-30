package com.wildtrack.repository;

import com.wildtrack.model.EmailVerification;
import com.wildtrack.model.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByEmailAndPurposeOrderByCreatedAtDesc(
            String email, VerificationPurpose purpose);

    void deleteByEmailAndPurpose(String email, VerificationPurpose purpose);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}