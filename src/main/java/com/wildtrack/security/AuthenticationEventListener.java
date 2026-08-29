package com.wildtrack.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        clientIp(event.getAuthentication()).ifPresent(loginAttemptService::recordFailure);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        clientIp(event.getAuthentication()).ifPresent(loginAttemptService::recordSuccess);
    }

    private Optional<String> clientIp(Authentication authentication) {
        return authentication.getDetails() instanceof String clientIp
                ? Optional.of(clientIp)
                : Optional.empty();
    }
}