package com.wildtrack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wildTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WildTrack API")
                        .description("REST API for tracking and managing wildlife observations")
                        .version("1.0.0"));
    }
}
