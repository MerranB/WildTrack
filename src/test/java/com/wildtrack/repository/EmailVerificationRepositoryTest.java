package com.wildtrack.repository;

import com.wildtrack.model.EmailVerification;
import com.wildtrack.model.VerificationPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmailVerificationRepositoryTest {

    private static final String EMAIL = "researcher@example.com";
    private static final String OTHER_EMAIL = "someone.else@example.com";
    private static final String PAYLOAD = "{\"name\":\"Test Fence\"}";

    @Autowired
    private EmailVerificationRepository repository;

    private EmailVerification save(String email, String code, VerificationPurpose purpose,
                                   LocalDateTime createdAt, LocalDateTime expiresAt) {
        return repository.saveAndFlush(new EmailVerification(
                email, code, purpose, PAYLOAD, createdAt, expiresAt));
    }

    private EmailVerification saveActive(String email, String code, VerificationPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        return save(email, code, purpose, now, now.plusMinutes(15));
    }

    @Test
    void findFirstByEmailAndPurpose_returnsNothing_whenNoneStored() {
        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE)).isEmpty();
    }

    @Test
    void findFirstByEmailAndPurpose_findsTheStoredRequest() {
        saveActive(EMAIL, "123456", VerificationPurpose.GEO_FENCE);

        Optional<EmailVerification> found = repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("123456");
        assertThat(found.get().getAttempts()).isZero();
    }

    @Test
    void findFirstByEmailAndPurpose_doesNotCrossPurposes() {
        saveActive(EMAIL, "123456", VerificationPurpose.DEMO);

        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE)).isEmpty();
    }

    @Test
    void findFirstByEmailAndPurpose_isScopedToOneAddress() {
        saveActive(OTHER_EMAIL, "123456", VerificationPurpose.GEO_FENCE);

        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE)).isEmpty();
    }

    @Test
    void findFirstByEmailAndPurpose_returnsTheNewestWhenSeveralExist() {
        LocalDateTime now = LocalDateTime.now();
        save(EMAIL, "111111", VerificationPurpose.GEO_FENCE, now.minusMinutes(10), now.plusMinutes(5));
        save(EMAIL, "222222", VerificationPurpose.GEO_FENCE, now, now.plusMinutes(15));

        Optional<EmailVerification> found = repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("222222");
    }

    @Test
    void deleteByEmailAndPurpose_removesOnlyThatAddressAndPurpose() {
        saveActive(EMAIL, "111111", VerificationPurpose.GEO_FENCE);
        saveActive(EMAIL, "222222", VerificationPurpose.DEMO);
        saveActive(OTHER_EMAIL, "333333", VerificationPurpose.GEO_FENCE);

        repository.deleteByEmailAndPurpose(EMAIL, VerificationPurpose.GEO_FENCE);

        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE)).isEmpty();
        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.DEMO)).isPresent();
        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                OTHER_EMAIL, VerificationPurpose.GEO_FENCE)).isPresent();
    }

    @Test
    void deleteByExpiresAtBefore_removesExpiredRowsAndKeepsLiveOnes() {
        LocalDateTime now = LocalDateTime.now();
        save(EMAIL, "111111", VerificationPurpose.GEO_FENCE, now.minusHours(2), now.minusHours(1));
        save(OTHER_EMAIL, "222222", VerificationPurpose.GEO_FENCE, now, now.plusMinutes(15));

        repository.deleteByExpiresAtBefore(now);

        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE)).isEmpty();
        assertThat(repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                OTHER_EMAIL, VerificationPurpose.GEO_FENCE)).isPresent();
    }

    @Test
    void attempts_persistAcrossReads() {
        EmailVerification saved = saveActive(EMAIL, "123456", VerificationPurpose.GEO_FENCE);
        saved.setAttempts(3);
        repository.saveAndFlush(saved);

        Optional<EmailVerification> found = repository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                EMAIL, VerificationPurpose.GEO_FENCE);

        assertThat(found).isPresent();
        assertThat(found.get().getAttempts()).isEqualTo(3);
    }
}
