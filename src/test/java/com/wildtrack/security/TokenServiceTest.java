package com.wildtrack.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TokenServiceTest {

    private static final String SECRET = "test-only-signing-secret-at-least-32-bytes";
    private static final String USERNAME = "test-admin";
    private static final long EXPIRY_MINUTES = 15L;

    private TokenService tokenService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        tokenService = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)));
        ReflectionTestUtils.setField(tokenService, "expiryMinutes", EXPIRY_MINUTES);

        jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private Authentication authenticationWith(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                USERNAME, "secret", AuthorityUtils.createAuthorityList(authorities));
    }

    private Jwt decode(String... authorities) {
        return jwtDecoder.decode(tokenService.generateToken(authenticationWith(authorities)));
    }

    @Test
    void generateToken_producesATokenThisKeyCanVerify() {
        assertThat(decode("ROLE_ADMIN")).isNotNull();
    }

    @Test
    void generateToken_setsTheUsernameAsSubject() {
        assertThat(decode("ROLE_ADMIN").getSubject()).isEqualTo(USERNAME);
    }

    @Test
    void generateToken_setsTheIssuer() {
        assertThat(decode("ROLE_ADMIN").getClaimAsString("iss")).isEqualTo("wildtrack");
    }

    /**
     * The claim carries bare role names because JwtGrantedAuthoritiesConverter puts the
     * ROLE_ prefix back on during decoding. Writing the prefix here would yield ROLE_ROLE_ADMIN.
     */
    @Test
    void generateToken_stripsTheRolePrefixFromTheClaim() {
        assertThat(decode("ROLE_ADMIN").getClaimAsStringList("roles")).containsExactly("ADMIN");
    }

    @Test
    void generateToken_omitsAuthoritiesThatAreNotRoles() {
        List<String> roles = decode("ROLE_ADMIN", "SCOPE_read", "SOMETHING_ELSE")
                .getClaimAsStringList("roles");

        assertThat(roles).containsExactly("ADMIN");
    }

    @Test
    void generateToken_expiresAfterTheConfiguredWindow() {
        Jwt jwt = decode("ROLE_ADMIN");

        long minutes = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toMinutes();

        assertThat(minutes).isEqualTo(EXPIRY_MINUTES);
    }

    @Test
    void generateToken_issuesTheTokenAtTheCurrentTime() {
        Jwt jwt = decode("ROLE_ADMIN");

        assertThat(jwt.getIssuedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void expirySeconds_reportsTheWindowTheClientShouldPlanAround() {
        assertThat(tokenService.expirySeconds()).isEqualTo(EXPIRY_MINUTES * 60);
    }
}
