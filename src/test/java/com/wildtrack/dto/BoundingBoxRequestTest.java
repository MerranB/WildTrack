package com.wildtrack.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoundingBoxRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void valid_request_passes_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(-10.0, -10.0, 10.0, 10.0))).isEmpty();
    }

    @Test
    void minLon_below_minus180_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(-181.0, 0.0, 0.0, 0.0))).isNotEmpty();
    }

    @Test
    void minLon_above_180_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(181.0, 0.0, 0.0, 0.0))).isNotEmpty();
    }

    @Test
    void minLat_below_minus90_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(0.0, -91.0, 0.0, 0.0))).isNotEmpty();
    }

    @Test
    void minLat_above_90_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(0.0, 91.0, 0.0, 0.0))).isNotEmpty();
    }

    @Test
    void maxLon_below_minus180_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(0.0, 0.0, -181.0, 0.0))).isNotEmpty();
    }

    @Test
    void maxLon_above_180_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(0.0, 0.0, 181.0, 0.0))).isNotEmpty();
    }

    @Test
    void maxLat_below_minus90_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(0.0, 0.0, 0.0, -91.0))).isNotEmpty();
    }

    @Test
    void maxLat_above_90_fails_validation() {
        assertThat(validator.validate(new BoundingBoxRequest(0.0, 0.0, 0.0, 91.0))).isNotEmpty();
    }
}