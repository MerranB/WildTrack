package com.wildtrack.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;


    @BeforeEach
    void setup() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
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
        @PostMapping("/bad-request")
        public void throwValidation(@Valid @RequestBody ValidationRequest request) {}
    }
    static class ValidationRequest {
        @NotBlank
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    void validation_exception_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/bad-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
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

