package com.wildtrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int STANDARD_LIMIT = 200;
    private static final int UPDATE_DATABASE_LIMIT = 2;
    private static final int NATURAL_QUERY_LIMIT = 2;
    private static final int DEMO_LIMIT = 2;
    private static final int TILE_LIMIT = 300;
    private static final int GEOFENCE_CREATE_LIMIT = 3;
    private static final int VERIFY_LIMIT = 10;

  // Never evicted, acceptable at this scale (single ECS instance, minimal unique visitors)
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = extractClientIp(request);
        String key = "";
        int limit = 0;
        String apiCalled = request.getRequestURI();
        String method = request.getMethod();
        if (apiCalled.contains("updateDatabase")) {
            key = ip + ":" + ("UPDATE_DATABASE_LIMIT");
            limit = UPDATE_DATABASE_LIMIT;
        } else if (apiCalled.contains("verify")) {
            key = ip + ":" + ("VERIFY_LIMIT");
            limit = VERIFY_LIMIT;
        } else if (apiCalled.contains("query")) {
            key = ip + ":" + ("NATURAL_QUERY_LIMIT");
            limit = NATURAL_QUERY_LIMIT;
        } else if (apiCalled.contains("demo")) {
            key = ip + ":" + ("DEMO_LIMIT");
            limit = DEMO_LIMIT;
        } else if (apiCalled.contains("geoFence") && "POST".equals(method)) {
            key = ip + ":" + ("GEOFENCE_CREATE_LIMIT");
            limit = GEOFENCE_CREATE_LIMIT;
        } else if (apiCalled.contains("tile")) {
            key = ip + ":" + ("TILE_LIMIT");
            limit = TILE_LIMIT;
        } else {
            key = ip + ":" + ("STANDARD_LIMIT");
            limit = STANDARD_LIMIT;
        }
        int effectiveLimit = limit;
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(effectiveLimit));

        if (bucket.tryConsume(1)) {
            return true;
        }

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Rate Limit Exceeded");
        problem.setDetail("You have exceeded the request limit for this endpoint. Please try again in 24 hours.");
        problem.setType(URI.create("about:blank"));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));

        return false;
    }

    private Bucket buildBucket(int limit) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillIntervally(limit, Duration.ofDays(1))
                        .build())
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
