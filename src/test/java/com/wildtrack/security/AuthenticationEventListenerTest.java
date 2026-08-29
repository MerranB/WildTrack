package com.wildtrack.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthenticationEventListenerTest {

    private static final String CLIENT_IP = "203.0.113.10";

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthenticationEventListener authenticationEventListener;

    private Authentication authenticationWithDetails(Object details) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("test-admin", "secret",
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        authentication.setDetails(details);
        return authentication;
    }

    @Test
    void onFailure_recordsTheAttemptAgainstTheResolvedAddress() {
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                authenticationWithDetails(CLIENT_IP), new BadCredentialsException("Bad credentials"));

        authenticationEventListener.onFailure(event);

        verify(loginAttemptService).recordFailure(CLIENT_IP);
    }

    @Test
    void onSuccess_clearsTheCountForThatAddress() {
        AuthenticationSuccessEvent event =
                new AuthenticationSuccessEvent(authenticationWithDetails(CLIENT_IP));

        authenticationEventListener.onSuccess(event);

        verify(loginAttemptService).recordSuccess(CLIENT_IP);
    }

    /**
     * Token authentications on ordinary admin requests also raise success events. They carry
     * servlet details rather than a resolved address, and must not disturb the lockout counters.
     */
    @Test
    void onSuccess_withNonLoginDetails_leavesTheCountersAlone() {
        WebAuthenticationDetails servletDetails =
                new WebAuthenticationDetails(new MockHttpServletRequest());
        AuthenticationSuccessEvent event =
                new AuthenticationSuccessEvent(authenticationWithDetails(servletDetails));

        authenticationEventListener.onSuccess(event);

        verifyNoInteractions(loginAttemptService);
    }

    @Test
    void onFailure_withoutDetails_leavesTheCountersAlone() {
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                authenticationWithDetails(null), new BadCredentialsException("Bad credentials"));

        authenticationEventListener.onFailure(event);

        verifyNoInteractions(loginAttemptService);
    }
}
