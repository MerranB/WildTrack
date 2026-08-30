package com.wildtrack.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.email.EmailDetail;
import com.wildtrack.email.EmailService;
import com.wildtrack.exception.VerificationCodeException;
import com.wildtrack.model.EmailVerification;
import com.wildtrack.model.VerificationPurpose;
import com.wildtrack.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int CODE_BOUND = 1_000_000;
    private static final String CODE_FORMAT = "%06d";

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${wildtrack.verification.code-expiry-minutes}")
    private long codeExpiryMinutes;

    @Value("${wildtrack.verification.max-attempts}")
    private int maxAttempts;

    @Transactional
    public void startVerification(String email, VerificationPurpose purpose, Object payload) {
        emailVerificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        // Only the newest code for an address should work, so retire any earlier one.
        emailVerificationRepository.deleteByEmailAndPurpose(email, purpose);

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();

        emailVerificationRepository.save(new EmailVerification(
                email, code, purpose, serialize(payload), now, now.plusMinutes(codeExpiryMinutes)));

        emailService.sendSimpleMail(new EmailDetail(
                email,
                "Your WildTrack confirmation code is " + code + ".\n\n"
                        + "It expires in " + codeExpiryMinutes + " minutes. Enter it in the API Explorer "
                        + "to finish your request.\n\n",
                "WildTrack confirmation code",
                null));

        log.info("Verification code issued for purpose {}", purpose);
    }

    @Transactional
    public <T> T consume(String email, String code, VerificationPurpose purpose, Class<T> payloadType) {
        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new VerificationCodeException(
                        "No pending request for that email address. Submit the request again to get a new code."));

        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            emailVerificationRepository.delete(verification);
            throw new VerificationCodeException(
                    "That code has expired. Submit the request again to get a new one.");
        }

        if (!matches(verification.getCode(), code)) {
            verification.setAttempts(verification.getAttempts() + 1);
            int remaining = maxAttempts - verification.getAttempts();

            if (remaining <= 0) {
                emailVerificationRepository.delete(verification);
                throw new VerificationCodeException(
                        "Too many incorrect codes. Submit the request again to get a new one.");
            }

            emailVerificationRepository.save(verification);
            throw new VerificationCodeException(
                    "Incorrect code. " + remaining + " attempt(s) remaining before it is cancelled.");
        }

        T payload = deserialize(verification.getPayload(), payloadType);
        emailVerificationRepository.delete(verification);
        return payload;
    }

    private String generateCode() {
        return String.format(CODE_FORMAT, secureRandom.nextInt(CODE_BOUND));
    }

    private boolean matches(String expected, String supplied) {
        return Optional.ofNullable(supplied)
                .map(value -> MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        value.trim().getBytes(StandardCharsets.UTF_8)))
                .orElse(false);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store the pending request", e);
        }
    }

    private <T> T deserialize(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read back the pending request", e);
        }
    }
}