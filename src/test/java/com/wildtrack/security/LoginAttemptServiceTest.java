package com.wildtrack.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String OTHER_IP = "203.0.113.99";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 60L;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
        setLockoutMinutes(LOCKOUT_MINUTES);
    }

    private void setLockoutMinutes(long minutes) {
        ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(loginAttemptService, "lockoutMinutes", minutes);
    }

    private void failTimes(String clientIp, int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            loginAttemptService.recordFailure(clientIp);
        }
    }

    @Test
    void isBlocked_withNoRecordedFailures_isFalse() {
        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isFalse();
    }

    @Test
    void isBlocked_belowTheThreshold_isFalse() {
        failTimes(CLIENT_IP, MAX_ATTEMPTS - 1);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isFalse();
    }

    @Test
    void isBlocked_atTheThreshold_isTrue() {
        failTimes(CLIENT_IP, MAX_ATTEMPTS);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isTrue();
    }

    @Test
    void isBlocked_beyondTheThreshold_staysTrue() {
        failTimes(CLIENT_IP, MAX_ATTEMPTS + 3);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isTrue();
    }

    @Test
    void recordSuccess_clearsTheFailureCount() {
        failTimes(CLIENT_IP, MAX_ATTEMPTS - 1);
        loginAttemptService.recordSuccess(CLIENT_IP);
        failTimes(CLIENT_IP, MAX_ATTEMPTS - 1);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isFalse();
    }

    @Test
    void recordSuccess_releasesAnActiveLockout() {
        failTimes(CLIENT_IP, MAX_ATTEMPTS);
        loginAttemptService.recordSuccess(CLIENT_IP);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isFalse();
    }

    @Test
    void isBlocked_afterTheLockoutWindowHasPassed_isFalseAgain() {
        setLockoutMinutes(-1L);
        failTimes(CLIENT_IP, MAX_ATTEMPTS);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isFalse();
    }

    @Test
    void lockout_isScopedToASingleAddress() {
        failTimes(CLIENT_IP, MAX_ATTEMPTS);

        assertThat(loginAttemptService.isBlocked(CLIENT_IP)).isTrue();
        assertThat(loginAttemptService.isBlocked(OTHER_IP)).isFalse();
    }
}
