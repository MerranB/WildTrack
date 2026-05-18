package com.wildtrack.mapper;

import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.model.GeoFence;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeoFenceMapperTest {

    private final GeoFenceMapper mapper = Mappers.getMapper(GeoFenceMapper.class);
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Polygon testPolygon() {
        Coordinate[] coords = {
                new Coordinate(1.0, 1.0), new Coordinate(-1.0, 1.0),
                new Coordinate(-1.0, -1.0), new Coordinate(1.0, -1.0),
                new Coordinate(1.0, 1.0)
        };
        return geometryFactory.createPolygon(coords);
    }

    private List<CoordinateDto> testCoordinates() {
        return List.of(
                new CoordinateDto(1.0, 1.0), new CoordinateDto(1.0, -1.0),
                new CoordinateDto(-1.0, -1.0), new CoordinateDto(-1.0, 1.0),
                new CoordinateDto(1.0, 1.0)
        );
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        GeoFenceDto dto = new GeoFenceDto("Test Fence", testCoordinates(), "test@example.com", "testuser", 0);

        GeoFence entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Test Fence");
        assertThat(entity.getEmail()).isEqualTo("test@example.com");
        assertThat(entity.getUsername()).isEqualTo("testuser");
        assertThat(entity.getLastAnimalCount()).isZero();
        assertThat(entity.getArea()).isNotNull();
    }

    @Test
    void toDto_mapsAllFieldsCorrectly() {
        GeoFence entity = new GeoFence("Test Fence", testPolygon(), "test@example.com", "testuser", 0);

        GeoFenceDto dto = mapper.toDto(entity);

        assertThat(dto.getName()).isEqualTo("Test Fence");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getUsername()).isEqualTo("testuser");
        assertThat(dto.getLastAnimalCount()).isZero();
        assertThat(dto.getCoordinates()).hasSize(5);
    }

    @Test
    void updateEntityFromDto_updatesFields() {
        GeoFence entity = new GeoFence("Old Name", testPolygon(), "old@example.com", "olduser", 0);
        GeoFenceDto dto = new GeoFenceDto("New Name", testCoordinates(), "new@example.com", "newuser", 0);

        mapper.updateEntityFromDto(dto, entity);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
        assertThat(entity.getUsername()).isEqualTo("newuser");
    }

    @Test
    void updateEntityFromDto_ignoresNullValues() {
        GeoFence entity = new GeoFence("Original Name", testPolygon(), "original@example.com", "originaluser", 0);
        GeoFenceDto dto = new GeoFenceDto(null, testCoordinates(), null, null, 0);

        mapper.updateEntityFromDto(dto, entity);

        assertThat(entity.getName()).isEqualTo("Original Name");
        assertThat(entity.getEmail()).isEqualTo("original@example.com");
        assertThat(entity.getUsername()).isEqualTo("originaluser");
    }

    @Test
    void polygonToCoordinates_returnsNull_whenPolygonIsNull() {
        assertThat(mapper.polygonToCoordinates(null)).isNull();
    }

    @Test
    void coordinatesToPolygon_returnsNull_whenCoordinatesIsNull() {
        assertThat(mapper.coordinatesToPolygon(null)).isNull();
    }
}