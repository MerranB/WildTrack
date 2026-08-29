package com.wildtrack.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";
    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR);

        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }

        String[] hops = forwardedFor.split(",");
        return hops[hops.length - 1].trim();
    }
}