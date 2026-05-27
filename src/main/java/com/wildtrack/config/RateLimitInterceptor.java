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

    private static final int STANDARD_LIMIT = 33;
    private static final int INTENSE_LIMIT = 2;

  // Never evicted — acceptable at this scale (single ECS instance, minimal unique visitors)
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = extractClientIp(request);
        boolean isIntense = request.getRequestURI().contains("updateDatabase");
        String key = ip + ":" + (isIntense ? "updateDatabase" : "standard");
        int limit = isIntense ? INTENSE_LIMIT : STANDARD_LIMIT;

        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(limit));

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
