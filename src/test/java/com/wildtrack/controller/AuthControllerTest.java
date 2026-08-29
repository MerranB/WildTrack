package com.wildtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.config.RateLimitInterceptor;
import com.wildtrack.dto.LoginRequest;
import com.wildtrack.security.ClientIpResolver;
import com.wildtrack.security.LoginAttemptService;
import com.wildtrack.security.TokenService;
import com.wildtrack.support.WithSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WithSecurityConfig
class AuthControllerTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String USERNAME = "test-admin";
    private static final String PASSWORD = "not-a-real-password";
    private static final String CLIENT_IP = "203.0.113.10";
    private static final String TOKEN = "a.signed.token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @Captor
    private ArgumentCaptor<Authentication> authenticationCaptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(clientIpResolver.resolve(any())).thenReturn(CLIENT_IP);
    }

    private String body(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(username, password));
    }

    private void stubSuccessfulAuthentication() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        USERNAME, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        when(tokenService.generateToken(any())).thenReturn(TOKEN);
        when(tokenService.expirySeconds()).thenReturn(900L);
    }

    @Test
    void login_withValidCredentials_returnsTheTokenAndItsLifetime() throws Exception {
        stubSuccessfulAuthentication();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.expiresInSeconds").value(900));
    }

    @Test
    void login_withBadCredentials_returnsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(USERNAME, "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBadCredentials_doesNotSayWhichFieldWasWrong() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(USERNAME, "wrong")))
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void login_whileLockedOut_returnsTooManyRequestsWithoutCheckingCredentials() throws Exception {
        when(loginAttemptService.isBlocked(CLIENT_IP)).thenReturn(true);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(USERNAME, PASSWORD)))
                .andExpect(status().isTooManyRequests());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_attachesTheResolvedClientAddressToTheAuthentication() throws Exception {
        stubSuccessfulAuthentication();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(USERNAME, PASSWORD)))
                .andExpect(status().isOk());

        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue().getDetails()).isEqualTo(CLIENT_IP);
    }

    @Test
    void login_withBlankUsername_returnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", PASSWORD)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_withBlankPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(USERNAME, "")))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }
}
