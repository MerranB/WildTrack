package com.wildtrack.controller;

import com.wildtrack.dto.LoginRequest;
import com.wildtrack.dto.LoginResponse;
import com.wildtrack.exception.TooManyLoginAttemptsException;
import com.wildtrack.security.ClientIpResolver;
import com.wildtrack.security.LoginAttemptService;
import com.wildtrack.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final ClientIpResolver clientIpResolver;

    @Operation(
            summary = "Exchange admin credentials for a short-lived access token",
            description = "Returns a signed token valid for 15 minutes. Send it on admin calls "
                    + "as an Authorization: Bearer header."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);

        if (loginAttemptService.isBlocked(clientIp)) {
            throw new TooManyLoginAttemptsException();
        }

        UsernamePasswordAuthenticationToken credentials =
                new UsernamePasswordAuthenticationToken(request.username(), request.password());
        credentials.setDetails(clientIp);

        Authentication authentication = authenticationManager.authenticate(credentials);

        return ResponseEntity.ok(new LoginResponse(
                tokenService.generateToken(authentication),
                tokenService.expirySeconds()));
    }
}