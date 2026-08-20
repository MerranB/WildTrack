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
            // Already canonical — the normalizer is a no-op on underscore headers,
            // which is why the original test CSV fixtures never exercised it.
            "timestamp,location_lat,location_long,individual_id,tag_id",
            // Columns the alias map has no entry for pass through untouched. Movebank
            // returns far more columns than the five fields the DTO binds.
            "event-id,visible,sensor-type,study-name",
            // The lookup is case-sensitive, so differently-cased headers are NOT mapped.
            // Documents current behaviour rather than endorsing it — move this value into
            // the mapping test if the aliases are ever made case-insensitive.
            "Location-Lat,TAG-LOCAL-IDENTIFIER"
    })
    void normalizeHeaders_leavesHeadersItCannotMapUnchanged(String raw) {
        assertThat(normalizer.normalizeHeaders(raw)).isEqualTo(raw);
    }

    // The aliases are exact-match, so a column that merely starts with the same prefix
    // must not be rewritten. Movebank sends individual-taxon-canonical-name alongside
    // individual-local-identifier, and only the latter is an identifier.
    private static Stream<Arguments> headerRewrites() {
        return Stream.of(
                // Every Movebank alias maps to the canonical name the DTO binds.
                Arguments.of(
                        "timestamp,location-long,location-lat,individual-local-identifier,tag-local-identifier",
                        "timestamp,location_long,location_lat,individual_id,tag_id"),
                // Aliases are exact-match — a column that merely shares a prefix is left alone.
                // Movebank sends individual-taxon-canonical-name next to the identifier.
                Arguments.of(
                        "individual-taxon-canonical-name,individual-local-identifier",
                        "individual-taxon-canonical-name,individual_id"),
                // Order is load-bearing: the data rows below the header are positional.
                Arguments.of(
                        "tag-local-identifier,timestamp,location-lat,event-id,location-long",
                        "tag_id,timestamp,location_lat,event-id,location_long"),
                // Surrounding whitespace is stripped before the alias lookup.
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
