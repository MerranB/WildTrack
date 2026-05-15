package com.wildtrack.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class CoordinateRangeRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void valid_request_passes_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(0.0, 0.0, 5.0))).isEmpty();
    }

    @Test
    void lon_below_minus180_fails_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(-181.0, 0.0, 5.0))).isNotEmpty();
    }

    @Test
    void lon_above_180_fails_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(181.0, 0.0, 5.0))).isNotEmpty();
    }

    @Test
    void lat_below_minus90_fails_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(0.0, -91.0, 5.0))).isNotEmpty();
    }

    @Test
    void lat_above_90_fails_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(0.0, 91.0, 5.0))).isNotEmpty();
    }

    @Test
    void range_below_0_fails_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(0.0, 0.0, -1.0))).isNotEmpty();
    }

    @Test
    void range_above_10_fails_validation() {
        assertThat(validator.validate(new com.wildtrack.dto.CoordinateRangeRequest(0.0, 0.0, 11.0))).isNotEmpty();
    }
}