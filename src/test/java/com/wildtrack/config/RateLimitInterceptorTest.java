package com.wildtrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final int GEOFENCE_CREATE_LIMIT = 3;
    private static final int DEMO_LIMIT = 2;

    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        rateLimitInterceptor = new RateLimitInterceptor(new ObjectMapper());
    }

    private boolean call(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(CLIENT_IP);

        return rateLimitInterceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private int allowedCallsBefore429(String method, String uri, int ceiling) throws Exception {
        int allowed = 0;
        for (int attempt = 0; attempt < ceiling; attempt++) {
            if (!call(method, uri)) {
                return allowed;
            }
            allowed++;
        }
        return allowed;
    }

    @Test
    void geoFenceCreate_isCappedTightlyBecauseItSendsEmail() throws Exception {
        assertThat(allowedCallsBefore429("POST", "/api/v1/geoFence", GEOFENCE_CREATE_LIMIT + 5))
                .isEqualTo(GEOFENCE_CREATE_LIMIT);
    }

    @Test
    void geoFenceReads_areNotCaughtByTheCreateLimit() throws Exception {
        for (int attempt = 0; attempt < GEOFENCE_CREATE_LIMIT + 5; attempt++) {
            assertThat(call("GET", "/api/v1/geoFence"))
                    .as("read %d should not be throttled by the create bucket", attempt)
                    .isTrue();
        }
    }

    @Test
    void demoRequest_keepsItsOwnTightLimit() throws Exception {
        assertThat(allowedCallsBefore429("POST", "/api/v1/demo", DEMO_LIMIT + 5))
                .isEqualTo(DEMO_LIMIT);
    }

    @Test
    void confirmingADemo_usesTheVerifyBudgetRatherThanTheDemoOne() throws Exception {
        for (int attempt = 0; attempt <= DEMO_LIMIT; attempt++) {
            assertThat(call("POST", "/api/v1/verify/demo"))
                    .as("verify attempt %d should not be spending the demo budget", attempt)
                    .isTrue();
        }
    }

    @Test
    void confirmingAGeoFence_usesTheVerifyBudgetRatherThanTheCreateOne() throws Exception {
        for (int attempt = 0; attempt <= GEOFENCE_CREATE_LIMIT; attempt++) {
            assertThat(call("POST", "/api/v1/verify/geoFence"))
                    .as("verify attempt %d should not be spending the create budget", attempt)
                    .isTrue();
        }
    }

    @Test
    void exceedingALimit_returnsTooManyRequestsAsProblemJson() throws Exception {
        for (int attempt = 0; attempt < DEMO_LIMIT; attempt++) {
            call("POST", "/api/v1/demo");
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/demo");
        request.setRemoteAddr(CLIENT_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = rateLimitInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Rate Limit Exceeded");
    }

    @Test
    void limitsAreScopedToOneAddress() throws Exception {
        for (int attempt = 0; attempt < DEMO_LIMIT; attempt++) {
            call("POST", "/api/v1/demo");
        }

        MockHttpServletRequest fromElsewhere = new MockHttpServletRequest("POST", "/api/v1/demo");
        fromElsewhere.setRemoteAddr("198.51.100.7");

        assertThat(rateLimitInterceptor.preHandle(
                fromElsewhere, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
