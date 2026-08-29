package com.wildtrack.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static final String FORWARDED_FOR = "X-Forwarded-For";
    private static final String PEER_ADDRESS = "203.0.113.10";
    private static final String LOAD_BALANCER_ADDRESS = "10.0.1.5";

    private final ClientIpResolver clientIpResolver = new ClientIpResolver();

    private MockHttpServletRequest requestWithForwardedFor(String headerValue) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(LOAD_BALANCER_ADDRESS);

        if (headerValue != null) {
            request.addHeader(FORWARDED_FOR, headerValue);
        }
        return request;
    }

    @Test
    void resolve_withoutForwardedForHeader_fallsBackToTheSocketAddress() {
        assertThat(clientIpResolver.resolve(requestWithForwardedFor(null)))
                .isEqualTo(LOAD_BALANCER_ADDRESS);
    }

    @Test
    void resolve_withBlankForwardedForHeader_fallsBackToTheSocketAddress() {
        assertThat(clientIpResolver.resolve(requestWithForwardedFor("   ")))
                .isEqualTo(LOAD_BALANCER_ADDRESS);
    }

    @Test
    void resolve_withSingleForwardedForEntry_returnsThatEntry() {
        assertThat(clientIpResolver.resolve(requestWithForwardedFor(PEER_ADDRESS)))
                .isEqualTo(PEER_ADDRESS);
    }

    @Test
    void resolve_trimsWhitespaceAroundTheEntry() {
        assertThat(clientIpResolver.resolve(requestWithForwardedFor("  " + PEER_ADDRESS + "  ")))
                .isEqualTo(PEER_ADDRESS);
    }

    /**
     * The load balancer appends the address it actually saw, so a caller who sends their own
     * X-Forwarded-For only prepends noise. Reading the last entry is what stops someone
     * inventing a fresh address per attempt to walk around the lockout.
     */
    @Test
    void resolve_withSpoofedLeadingEntries_returnsTheAddressTheLoadBalancerAppended() {
        String spoofed = "1.1.1.1, 2.2.2.2, " + PEER_ADDRESS;

        assertThat(clientIpResolver.resolve(requestWithForwardedFor(spoofed)))
                .isEqualTo(PEER_ADDRESS);
    }
}
