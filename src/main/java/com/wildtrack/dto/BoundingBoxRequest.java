package com.wildtrack.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record BoundingBoxRequest(
    @DecimalMin("-180") @DecimalMax("180") double minLon,
    @DecimalMin("-90") @DecimalMax("90")   double minLat,
    @DecimalMin("-180") @DecimalMax("180") double maxLon,
    @DecimalMin("-90") @DecimalMax("90") double maxLat
) {}