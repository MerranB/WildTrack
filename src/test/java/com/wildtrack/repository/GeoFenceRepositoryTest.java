package com.wildtrack.repository;

import com.wildtrack.model.GeoFence;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GeoFenceRepositoryTest {

    @Autowired
    GeoFenceRepository repository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Polygon samplePolygon() {
        Coordinate[] coords = {
                new Coordinate(20.0, 10.0),
                new Coordinate(21.0, 10.0),
                new Coordinate(21.0, 11.0),
                new Coordinate(20.0, 11.0),
                new Coordinate(20.0, 10.0)
        };
        return geometryFactory.createPolygon(coords);
    }

    @Test
    void existsByName_returnsTrue_whenNameExists() {
        repository.save(new GeoFence("Test Fence", samplePolygon(), "test@email.com", "testuser"));

        assertThat(repository.existsByName("Test Fence")).isTrue();
    }

    @Test
    void existsByName_returnsFalse_whenNameDoesNotExist() {
        assertThat(repository.existsByName("Nonexistent Fence")).isFalse();
    }
}