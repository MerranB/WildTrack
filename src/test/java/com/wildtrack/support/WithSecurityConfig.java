package com.wildtrack.support;

import com.wildtrack.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "wildtrack.security.admin.username=test-admin",
        "wildtrack.security.admin.password=not-a-real-password",
        "wildtrack.security.jwt.secret=test-only-signing-secret-at-least-32-bytes",
        "wildtrack.security.jwt.expiry-minutes=15",
        "wildtrack.security.lockout.max-attempts=5",
        "wildtrack.security.lockout.duration-minutes=60"
})
public @interface WithSecurityConfig {
}
