package com.wildtrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.email.EmailDetail;
import com.wildtrack.email.EmailService;
import com.wildtrack.exception.VerificationCodeException;
import com.wildtrack.model.EmailVerification;
import com.wildtrack.model.VerificationPurpose;
import com.wildtrack.repository.EmailVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "researcher@example.com";
    private static final String FENCE_NAME = "Test Fence";
    private static final long EXPIRY_MINUTES = 15L;
    private static final int MAX_ATTEMPTS = 5;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<EmailVerification> verificationCaptor;

    @Captor
    private ArgumentCaptor<EmailDetail> emailCaptor;

    private EmailVerificationService emailVerificationService;

    private static ObjectMapper payloadMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(
                emailVerificationRepository, emailService, payloadMapper());

        ReflectionTestUtils.setField(emailVerificationService, "codeExpiryMinutes", EXPIRY_MINUTES);
        ReflectionTestUtils.setField(emailVerificationService, "maxAttempts", MAX_ATTEMPTS);
    }

    private GeoFenceDto samplePayload() {
        return new GeoFenceDto(
                FENCE_NAME,
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(10.0, 20.0)),
                EMAIL, "researcher1", 0);
    }

    private EmailVerification pending(String code, LocalDateTime expiresAt, int attempts) {
        EmailVerification verification = new EmailVerification(
                EMAIL, code, VerificationPurpose.GEO_FENCE,
                serialisedPayload(), LocalDateTime.now(), expiresAt);
        verification.setAttempts(attempts);
        return verification;
    }

    private String serialisedPayload() {
        try {
            return payloadMapper().writeValueAsString(samplePayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void givenPending(EmailVerification verification) {
        when(emailVerificationRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(EMAIL, VerificationPurpose.GEO_FENCE))
                .thenReturn(Optional.of(verification));
    }

    private EmailVerification captureStarted() {
        emailVerificationService.startVerification(EMAIL, VerificationPurpose.GEO_FENCE, samplePayload());
        verify(emailVerificationRepository).save(verificationCaptor.capture());
        return verificationCaptor.getValue();
    }

    @Test
    void startVerification_storesASixDigitCode() {
        assertThat(captureStarted().getCode()).matches("\\d{6}");
    }

    @Test
    void startVerification_storesTheAddressAndPurpose() {
        EmailVerification saved = captureStarted();

        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getPurpose()).isEqualTo(VerificationPurpose.GEO_FENCE);
        assertThat(saved.getAttempts()).isZero();
    }

    @Test
    void startVerification_expiresAfterTheConfiguredWindow() {
        EmailVerification saved = captureStarted();

        long minutes = java.time.Duration.between(saved.getCreatedAt(), saved.getExpiresAt()).toMinutes();

        assertThat(minutes).isEqualTo(EXPIRY_MINUTES);
    }

    @Test
    void startVerification_retiresAnyEarlierCodeForThatAddress() {
        emailVerificationService.startVerification(EMAIL, VerificationPurpose.GEO_FENCE, samplePayload());

        verify(emailVerificationRepository).deleteByEmailAndPurpose(EMAIL, VerificationPurpose.GEO_FENCE);
        verify(emailVerificationRepository).deleteByExpiresAtBefore(any());
    }

    @Test
    void startVerification_mailsTheCodeToTheAddressGiven() {
        emailVerificationService.startVerification(EMAIL, VerificationPurpose.GEO_FENCE, samplePayload());

        verify(emailVerificationRepository).save(verificationCaptor.capture());
        verify(emailService).sendSimpleMail(emailCaptor.capture());
        EmailDetail sent = emailCaptor.getValue();

        assertThat(sent.recipient()).isEqualTo(EMAIL);
        assertThat(sent.msgBody()).contains(verificationCaptor.getValue().getCode());
    }

    @Test
    void startVerification_doesNotRepeatTheRequestDetailsBackToAnUnverifiedAddress() {
        emailVerificationService.startVerification(EMAIL, VerificationPurpose.GEO_FENCE, samplePayload());

        verify(emailService).sendSimpleMail(emailCaptor.capture());

        assertThat(emailCaptor.getValue().msgBody()).doesNotContain(FENCE_NAME);
    }

    @Test
    void consume_withTheRightCode_returnsTheStoredRequest() {
        givenPending(pending("123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), 0));

        GeoFenceDto payload = emailVerificationService.consume(
                EMAIL, "123456", VerificationPurpose.GEO_FENCE, GeoFenceDto.class);

        assertThat(payload.getName()).isEqualTo(FENCE_NAME);
        assertThat(payload.getEmail()).isEqualTo(EMAIL);
        assertThat(payload.getCoordinates()).hasSize(4);
    }

    @Test
    void consume_withTheRightCode_retiresTheRecordSoItCannotBeReplayed() {
        EmailVerification verification = pending("123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), 0);
        givenPending(verification);

        emailVerificationService.consume(EMAIL, "123456", VerificationPurpose.GEO_FENCE, GeoFenceDto.class);

        verify(emailVerificationRepository).delete(verification);
    }

    @Test
    void consume_toleratesSurroundingWhitespaceFromCopyAndPaste() {
        givenPending(pending("123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), 0));

        assertThat(emailVerificationService.consume(
                EMAIL, "  123456 ", VerificationPurpose.GEO_FENCE, GeoFenceDto.class)).isNotNull();
    }

    @Test
    void consume_withNoPendingRequest_isRejected() {
        when(emailVerificationRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(EMAIL, VerificationPurpose.GEO_FENCE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.consume(
                EMAIL, "123456", VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("No pending request");
    }

    @Test
    void consume_afterTheCodeExpired_isRejectedAndTheRecordDropped() {
        EmailVerification expired = pending("123456", LocalDateTime.now().minusMinutes(1), 0);
        givenPending(expired);

        assertThatThrownBy(() -> emailVerificationService.consume(
                EMAIL, "123456", VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("expired");

        verify(emailVerificationRepository).delete(expired);
    }

    @Test
    void consume_withTheWrongCode_countsTheAttemptAndKeepsTheRequest() {
        EmailVerification verification = pending("123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), 0);
        givenPending(verification);

        assertThatThrownBy(() -> emailVerificationService.consume(
                EMAIL, "000000", VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("4 attempt(s) remaining");

        assertThat(verification.getAttempts()).isEqualTo(1);
        // The stored request survives, so the caller never re-enters their coordinates.
        verify(emailVerificationRepository).save(verification);
        verify(emailVerificationRepository, never()).delete(verification);
    }

    @Test
    void consume_onTheFinalWrongAttempt_cancelsTheRequest() {
        EmailVerification verification = pending(
                "123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), MAX_ATTEMPTS - 1);
        givenPending(verification);

        assertThatThrownBy(() -> emailVerificationService.consume(
                EMAIL, "000000", VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("Too many incorrect codes");

        verify(emailVerificationRepository).delete(verification);
    }

    @Test
    void consume_withANullCode_isTreatedAsWrongRatherThanFailing() {
        EmailVerification verification = pending("123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), 0);
        givenPending(verification);

        assertThatThrownBy(() -> emailVerificationService.consume(
                EMAIL, null, VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("Incorrect code");
    }

    @Test
    void consume_countsEveryWrongAttemptTowardsTheCap() {
        EmailVerification verification = pending("123456", LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), 0);
        givenPending(verification);

        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            int remaining = MAX_ATTEMPTS - attempt;
            assertThatThrownBy(() -> emailVerificationService.consume(
                    EMAIL, "000000", VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                    .hasMessageContaining(remaining + " attempt(s) remaining");
        }

        assertThatThrownBy(() -> emailVerificationService.consume(
                EMAIL, "000000", VerificationPurpose.GEO_FENCE, GeoFenceDto.class))
                .hasMessageContaining("Too many incorrect codes");
    }
}
