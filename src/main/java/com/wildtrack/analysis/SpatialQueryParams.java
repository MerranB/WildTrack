package com.wildtrack.analysis;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpatialQueryParams(
    @JsonProperty("longitude")
    double longitude,
    @JsonProperty("latitude")
    double latitude,
    @JsonProperty("range")
    double range,
    @JsonProperty("startDate")
    LocalDate  startDate,
    @JsonProperty("endDate")
    LocalDate  endDate
)
{}