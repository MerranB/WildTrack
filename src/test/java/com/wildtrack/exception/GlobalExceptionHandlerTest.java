package com.wildtrack.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class TestController {
        @GetMapping("/test-not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Study not found");
        }
        @GetMapping("/movebank-api-limit")
        public void throwMovebankAPIRatelimit() {
            throw new MovebankRateLimitException();
        }
        @GetMapping("/movebank-error")
        public void throwMovebankAPIError() {
            throw new MovebankApiException();
        }
    }

    @Test
    void resource_not_found_exception_returns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/test-not-found"))
                .andExpect(status().isNotFound());
    }
    @Test
    void movebank_api_rate_limit() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/movebank-api-limit"))
                .andExpect(status().isTooManyRequests());
    }
    @Test
    void movebank_api_error() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/movebank-error"))
                .andExpect(status().isBadGateway());
    }
}