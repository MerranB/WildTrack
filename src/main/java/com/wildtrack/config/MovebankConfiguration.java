package com.wildtrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class MovebankConfiguration {

    @Bean
    public RestClient restClient(RestClient.Builder builder,
                                 @Value("${movebank.username}") String mbUsername,
                                 @Value("${movebank.password}") String mbPassword) {

        String encodedAuth = Base64.getEncoder().encodeToString((mbUsername + ":" + mbPassword).getBytes(StandardCharsets.UTF_8));
        return builder.baseUrl("https://www.movebank.org/")
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
    }
}