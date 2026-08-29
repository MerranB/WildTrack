package com.wildtrack.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.config.RateLimitInterceptor;
import com.wildtrack.dto.LoginRequest;
import com.wildtrack.service.MovebankEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "wildtrack.security.admin.username=integration-admin",
        "wildtrack.security.admin.password=integration-only-password",
        "wildtrack.security.jwt.secret=integration-signing-secret-at-least-32-bytes",
        "wildtrack.security.jwt.expiry-minutes=15",
        "wildtrack.security.lockout.max-attempts=5",
        "wildtrack.security.lockout.duration-minutes=60"
})
class SecurityIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ADMIN_URL = "/api/v1/events/updateDatabase";
    private static final String PUBLIC_URL = "/api/v1/events/hotspots";
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private static final String USERNAME = "integration-admin";
    private static final String PASSWORD = "integration-only-password";
    private static final String WRONG_PASSWORD = "the-wrong-password";
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovebankEventService movebankEventService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private String loginBody(String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(USERNAME, password));
    }

    private JsonNode successfulLogin() throws Exception {
        String response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private void failLogin(String clientIp) throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .header(FORWARDED_FOR, clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(WRONG_PASSWORD)));
    }

    private void lockOut(String clientIp) throws Exception {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            failLogin(clientIp);
        }
    }

    @Test
    void login_withValidCredentials_returnsASignedTokenAndItsLifetime() throws Exception {
        JsonNode json = successfulLogin();

        assertThat(json.get("token").asText()).isNotBlank();
        assertThat(json.get("expiresInSeconds").asLong()).isEqualTo(900L);
    }

    @Test
    void adminEndpoint_withATokenObtainedByLoggingIn_isAllowedThrough() throws Exception {
        when(movebankEventService.updateDatabase()).thenReturn("FULL_SUCCESS for 19186107\n");
        String token = successfulLogin().get("token").asText();

        mockMvc.perform(post(ADMIN_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoint_withNoToken_isRejected() throws Exception {
        mockMvc.perform(post(ADMIN_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withATokenThisServerDidNotSign_isRejected() throws Exception {
        mockMvc.perform(post(ADMIN_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoint_needsNoToken() throws Exception {
        when(movebankEventService.hotspots()).thenReturn(List.of());

        mockMvc.perform(get(PUBLIC_URL))
                .andExpect(status().isOk());
    }

    @Test
    void login_withTheWrongPassword_isRejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .header(FORWARDED_FOR, "198.51.100.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_afterRepeatedFailuresFromOneAddress_isLockedOut() throws Exception {
        String clientIp = "198.51.100.2";
        lockOut(clientIp);

        mockMvc.perform(post(LOGIN_URL)
                        .header(FORWARDED_FOR, clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void lockout_doesNotSpreadToOtherAddresses() throws Exception {
        lockOut("198.51.100.3");

        mockMvc.perform(post(LOGIN_URL)
                        .header(FORWARDED_FOR, "198.51.100.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk());
    }

    /**
     * A caller can put anything at the front of X-Forwarded-For, but the load balancer appends
     * the address it actually saw. Prepending a fresh fake address must not buy another five
     * attempts, or the lockout is decorative.
     */
    @Test
    void lockout_survivesASpoofedForwardedForPrefix() throws Exception {
        String clientIp = "198.51.100.5";
        lockOut(clientIp);

        mockMvc.perform(post(LOGIN_URL)
                        .header(FORWARDED_FOR, "1.1.1.1, 2.2.2.2, " + clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }
}
