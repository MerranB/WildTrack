package com.wildtrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Configuration
public class MovebankConfiguration {

    @Value("${movebank.username}")
    private String mbUsername;

    @Value("${movebank.password}")
    private String mbPassword;

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        String encodedAuth = Base64.getEncoder().encodeToString((mbUsername + ":" + mbPassword).getBytes());
        return builder.baseUrl("https://www.movebank.org/")
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
    }
}