package com.wildtrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "wildtrack.security.admin.username=test-admin",
        "wildtrack.security.admin.password=not-a-real-password",
        "wildtrack.security.jwt.secret=test-only-signing-secret-at-least-32-bytes"
})
class ApplicationTests {

    @Test
    void contextLoads() {
            // Verifies the Spring application context loads successfully without errors
    }
}

