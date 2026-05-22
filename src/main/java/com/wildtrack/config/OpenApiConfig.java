package com.wildtrack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wildTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WildTrack API")
                        .description("REST API for wildlife telemetry analysis. Includes CRUD endpoints for tracking events and geo-fences, a Movebank ingestion pipeline, and an AI-powered natural language query interface backed by Claude Haiku 4.5 and PostGIS spatial queries.")
                        .version("1.0.0"));
    }
    @Bean
    public OperationCustomizer pageableParameterCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) return operation;
            operation.getParameters().forEach(param -> {
                if ("page".equals(param.getName())) {
                    param.setExample(0);
                } else if ("size".equals(param.getName())) {
                    param.setExample(20);
                } else if ("sort".equals(param.getName())) {
                    param.setExample("timestamp,asc");
                    param.setSchema(new Schema<String>().type("string"));
                }
            });
            return operation;
        };
    }
}
