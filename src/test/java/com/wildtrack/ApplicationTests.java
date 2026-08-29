package com.wildtrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
            // Verifies the Spring application context loads successfully without errors
    }
}

