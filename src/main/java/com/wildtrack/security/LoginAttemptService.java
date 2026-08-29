package com.wildtrack.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int PURGE_THRESHOLD = 1000;

    private final Map<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    @Value("${wildtrack.security.lockout.max-attempts}")
    private int maxAttempts;

    @Value("${wildtrack.security.lockout.duration-minutes}")
    private long lockoutMinutes;

    private record Attempts(int count, Instant lockedUntil) {
        boolean isExpired(Instant now) {
            return lockedUntil != null && now.isAfter(lockedUntil);
        }
    }

    public boolean isBlocked(String clientIp) {
        Attempts attempts = attemptsByIp.get(clientIp);

        if (attempts == null || attempts.lockedUntil() == null) {
            return false;
        }
        if (attempts.isExpired(Instant.now())) {
            attemptsByIp.remove(clientIp);
            return false;
        }
        return true;
    }

    public void recordFailure(String clientIp) {
        if (attemptsByIp.size() > PURGE_THRESHOLD) {
            purgeExpired();
        }

        attemptsByIp.compute(clientIp, (ip, existing) -> {
            int count = existing == null ? 1 : existing.count() + 1;
            Instant lockedUntil = count >= maxAttempts
                    ? Instant.now().plus(Duration.ofMinutes(lockoutMinutes))
                    : null;

            if (lockedUntil != null) {
                log.warn("Locked out {} until {} after {} failed login attempts", ip, lockedUntil, count);
            }
            return new Attempts(count, lockedUntil);
        });
    }

    public void recordSuccess(String clientIp) {
        attemptsByIp.remove(clientIp);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        attemptsByIp.values().removeIf(attempts -> attempts.isExpired(now));
    }
}