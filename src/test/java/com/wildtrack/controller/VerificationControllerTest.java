package com.wildtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.config.RateLimitInterceptor;
import com.wildtrack.dto.VerificationRequest;
import com.wildtrack.exception.VerificationCodeException;
import com.wildtrack.service.VerifiedActionService;
import com.wildtrack.support.WithSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationController.class)
@WithSecurityConfig
class VerificationControllerTest {

    private static final String VERIFY_GEOFENCE_URL = "/api/v1/verify/geoFence";
    private static final String VERIFY_DEMO_URL = "/api/v1/verify/demo";
    private static final String EMAIL = "researcher@example.com";
    private static final String CODE = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VerifiedActionService verifiedActionService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private String body(String email, String code) throws Exception {
        return objectMapper.writeValueAsString(new VerificationRequest(email, code));
    }

    @Test
    void verifyGeoFence_withTheRightCode_reportsTheFenceWasCreated() throws Exception {
        when(verifiedActionService.completeGeoFence(EMAIL, CODE))
                .thenReturn("Email confirmed. Geo-fence 12 has been created.");

        mockMvc.perform(post(VERIFY_GEOFENCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, CODE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Geo-fence 12")));
    }

    @Test
    void verifyDemo_withTheRightCode_reportsTheDemoRan() throws Exception {
        when(verifiedActionService.completeDemo(EMAIL, CODE))
                .thenReturn("Email confirmed. Your geofence has been successfully setup!");

        mockMvc.perform(post(VERIFY_DEMO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, CODE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Email confirmed")));
    }

    @Test
    void verifyGeoFence_withTheWrongCode_returnsBadRequestSayingWhatIsLeft() throws Exception {
        when(verifiedActionService.completeGeoFence(EMAIL, CODE))
                .thenThrow(new VerificationCodeException("Incorrect code. 4 attempt(s) remaining."));

        mockMvc.perform(post(VERIFY_GEOFENCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("4 attempt(s) remaining")));
    }

    @Test
    void verifyGeoFence_afterTheCodeExpired_returnsBadRequest() throws Exception {
        when(verifiedActionService.completeGeoFence(EMAIL, CODE))
                .thenThrow(new VerificationCodeException("That code has expired."));

        mockMvc.perform(post(VERIFY_GEOFENCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("expired")));
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "researcher@example.com, 12345,     a code that is too short",
            "researcher@example.com, 1234567,   a code that is too long",
            "researcher@example.com, abcdef,    a code that is not numeric",
            "not-an-email,           123456,    an address that is not valid"
    })
    void verify_withMalformedInput_returnsBadRequestWithoutConsultingTheService(
            String email, String code, String scenario) throws Exception {
        mockMvc.perform(post(VERIFY_GEOFENCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, code)))
                .andExpect(status().isBadRequest());

        verify(verifiedActionService, never()).completeGeoFence(any(), any());
    }

    @Test
    void verify_isPublic_soAVisitorCanConfirmTheirOwnAddress() throws Exception {
        when(verifiedActionService.completeDemo(EMAIL, CODE)).thenReturn("done");

        mockMvc.perform(post(VERIFY_DEMO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, CODE)))
                .andExpect(status().isOk());
    }
}
