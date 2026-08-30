package com.wildtrack.service;

import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.email.EmailService;
import com.wildtrack.exception.VerificationCodeException;
import com.wildtrack.model.EmailVerification;
import com.wildtrack.model.VerificationPurpose;
import com.wildtrack.repository.EmailVerificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs consume() through the real Spring proxy against a real database, because the thing
 * being checked is whether a failed attempt survives the transaction.
 *
 * VerificationCodeException is unchecked, so without noRollbackFor Spring rolls the
 * transaction back on the way out and discards the very increment that was just recorded.
 * The attempt counter then sits at zero forever, the cap never fires, and a six digit code
 * becomes unlimited guesses. A mocked repository cannot see any of that: the increment is
 * on an object the test itself is holding, so it looks like it worked.
 *
 * The class is deliberately not @Transactional. That would wrap every call in one outer
 * transaction and hide exactly the boundary under test.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "wildtrack.security.admin.username=integration-admin",
        "wildtrack.security.admin.password=integration-only-password",
        "wildtrack.security.jwt.secret=integration-signing-secret-at-least-32-bytes",
        "wildtrack.verification.code-expiry-minutes=15",
        "wildtrack.verification.max-attempts=5"
})
class EmailVerificationTransactionTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final String WRONG_CODE = "000000";

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailService emailService;

    /** A fresh address per test, since @SpringBootTest does not roll rows back afterwards. */
    private String uniqueEmail() {
        return "verify-" + UUID.randomUUID() + "@example.com";
    }

    private GeoFenceDto samplePayload(String email) {
        return new GeoFenceDto(
                "Test Fence",
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(10.0, 20.0)),
                email, "researcher1", 0);
    }

    private String startAndReadCode(String email) {
        emailVerificationService.startVerification(email, VerificationPurpose.GEO_FENCE, samplePayload(email));
        return stored(email).getCode();
    }

    private EmailVerification stored(String email) {
        return emailVerificationRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, VerificationPurpose.GEO_FENCE)
                .orElseThrow(() -> new AssertionError("no pending verification for " + email));
    }

    private void guessWrong(String email) {
        assertThatThrownBy(() -> emailVerificationService.consume(
                email, WRONG_CODE, VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class);
    }

    @Test
    void aWrongAttempt_isStillCountedAfterTheExceptionUnwinds() {
        String email = uniqueEmail();
        startAndReadCode(email);

        guessWrong(email);

        assertThat(stored(email).getAttempts())
                .as("the increment must outlive the exception that reported it")
                .isEqualTo(1);
    }

    @Test
    void attemptsRemaining_countsDownAcrossSeparateCalls() {
        String email = uniqueEmail();
        startAndReadCode(email);

        guessWrong(email);

        assertThatThrownBy(() -> emailVerificationService.consume(
                email, WRONG_CODE, VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .hasMessageContaining("3 attempt(s) remaining");
    }

    @Test
    void theAttemptCap_actuallyFires() {
        String email = uniqueEmail();
        startAndReadCode(email);

        for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
            guessWrong(email);
        }

        assertThatThrownBy(() -> emailVerificationService.consume(
                email, WRONG_CODE, VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("Too many incorrect codes");
    }

    @Test
    void exhaustingTheAttempts_removesTheRequest() {
        String email = uniqueEmail();
        startAndReadCode(email);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            guessWrong(email);
        }

        assertThat(emailVerificationRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                email, VerificationPurpose.GEO_FENCE)).isEmpty();
    }

    @Test
    void theRightCode_stillWorksAfterSomeWrongOnes() {
        String email = uniqueEmail();
        String code = startAndReadCode(email);

        guessWrong(email);
        guessWrong(email);

        GeoFenceDto payload = emailVerificationService.consume(
                email, code, VerificationPurpose.GEO_FENCE, GeoFenceDto.class);

        assertThat(payload.getEmail()).isEqualTo(email);
        assertThat(emailVerificationRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                email, VerificationPurpose.GEO_FENCE)).isEmpty();
    }

    /** The delete on the expiry path rides on the same rolled back transaction. */
    @Test
    void anExpiredRequest_isRemovedRatherThanLeftBehind() {
        String email = uniqueEmail();
        startAndReadCode(email);

        EmailVerification expired = stored(email);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        emailVerificationRepository.saveAndFlush(expired);

        assertThatThrownBy(() -> emailVerificationService.consume(
                email, WRONG_CODE, VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .hasMessageContaining("expired");

        assertThat(emailVerificationRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                email, VerificationPurpose.GEO_FENCE)).isEmpty();
    }

    @Test
    void aNewCode_resetsTheAttemptCount() {
        String email = uniqueEmail();
        startAndReadCode(email);
        guessWrong(email);
        guessWrong(email);

        startAndReadCode(email);

        assertThat(stored(email).getAttempts()).isZero();
    }
}
