package com.wildtrack.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MovebankHeaderNormalizerTest {

    private final MovebankHeaderNormalizer normalizer = new MovebankHeaderNormalizer();

    @ParameterizedTest
    @ValueSource(strings = {
            "timestamp,location_lat,location_long,individual_id,tag_id",
            "event-id,visible,sensor-type,study-name",
            "Location-Lat,TAG-LOCAL-IDENTIFIER"
    })
    void normalizeHeaders_leavesHeadersItCannotMapUnchanged(String raw) {
        assertThat(normalizer.normalizeHeaders(raw)).isEqualTo(raw);
    }

    private static Stream<Arguments> headerRewrites() {
        return Stream.of(
                // Every Movebank alias maps to the canonical name the DTO binds.
                Arguments.of(
                        "timestamp,location-long,location-lat,individual-local-identifier,tag-local-identifier",
                        "timestamp,location_long,location_lat,individual_id,tag_id"),
                Arguments.of(
                        "individual-taxon-canonical-name,individual-local-identifier",
                        "individual-taxon-canonical-name,individual_id"),
                Arguments.of(
                        "tag-local-identifier,timestamp,location-lat,event-id,location-long",
                        "tag_id,timestamp,location_lat,event-id,location_long"),
                Arguments.of(
                        " timestamp , location-lat ,  tag-local-identifier ",
                        "timestamp,location_lat,tag_id")
        );
    }

    @ParameterizedTest
    @MethodSource("headerRewrites")
    void normalizeHeaders_rewritesAliasesToCanonicalNames(String raw, String expected) {
        assertThat(normalizer.normalizeHeaders(raw)).isEqualTo(expected);
    }

    @Test
    void normalizeHeaders_handlesSingleColumn() {
        assertThat(normalizer.normalizeHeaders("location-lat")).isEqualTo("location_lat");
    }

    @Test
    void normalizeHeaders_handlesEmptyString() {
        assertThat(normalizer.normalizeHeaders("")).isEmpty();
    }

    @Test
    void normalizeHeaders_mapsRealMovebankHeaderLine() {
        String raw = "event-id,visible,timestamp,location-long,location-lat,sensor-type,"
                + "individual-taxon-canonical-name,tag-local-identifier,individual-local-identifier,study-name";

        String result = normalizer.normalizeHeaders(raw);

        assertThat(result).isEqualTo("event-id,visible,timestamp,location_long,location_lat,sensor-type,"
                + "individual-taxon-canonical-name,tag_id,individual_id,study-name");
    }

}
