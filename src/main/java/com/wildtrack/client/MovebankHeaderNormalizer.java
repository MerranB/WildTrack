package com.wildtrack.client;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class MovebankHeaderNormalizer {

    private static final Map<String, String> ALIASES = Map.of(
            "location-long", "location_long",
            "location-lat", "location_lat",
            "individual-local-identifier", "individual_id",
            "tag-local-identifier", "tag_id"
    );

    public String normalizeHeaders(String headers){

        return Arrays.stream(headers.split(","))
                .map(String::trim)
                .map(token -> ALIASES.getOrDefault(token, token))
                .collect(Collectors.joining(","));
    }
}
