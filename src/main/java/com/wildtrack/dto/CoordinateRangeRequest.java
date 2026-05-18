package com.wildtrack.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record CoordinateRangeRequest(
    @DecimalMin("-180") @DecimalMax("180") double lon,
    @DecimalMin("-90") @DecimalMax("90")   double lat,
    @DecimalMin("0") @DecimalMax("10") double range
) {}