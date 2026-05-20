package com.wildtrack.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpatialQueryParams(
    @JsonProperty("longitude")
    double longitude,
    @JsonProperty("latitude")
    double latitude,
    @JsonProperty("range")
    double range,
    @JsonProperty("locationType")
    String locationType,
    @JsonProperty("confidence")
    String confidence
)
{}