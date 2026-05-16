package com.wildtrack.mapper;

import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.model.GeoFence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GeoFenceMapperTest {

    private final GeoFenceMapper mapper = Mappers.getMapper(GeoFenceMapper.class);
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

    private List<CoordinateDto> sampleCoordinates() {
        return List.of(
                new CoordinateDto(10.0, 20.0),
                new CoordinateDto(10.0, 21.0),
                new CoordinateDto(11.0, 21.0),
                new CoordinateDto(11.0, 20.0),
                new CoordinateDto(10.0, 20.0)
        );
    }

    @Test
    void toDto_mapsAllFieldsCorrectly() {
        GeoFence entity = new GeoFence("Test Fence", samplePolygon(), "test@email.com", "testuser");

        GeoFenceDto dto = mapper.toDto(entity);

        assertThat(dto.getName()).isEqualTo("Test Fence");
        assertThat(dto.getEmail()).isEqualTo("test@email.com");
        assertThat(dto.getUsername()).isEqualTo("testuser");
        assertThat(dto.getCoordinates()).isNotNull();
        assertThat(dto.getCoordinates()).hasSize(5);
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        GeoFenceDto dto = new GeoFenceDto("Test Fence", sampleCoordinates(), "test@email.com", "testuser");

        GeoFence entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Test Fence");
        assertThat(entity.getEmail()).isEqualTo("test@email.com");
        assertThat(entity.getUsername()).isEqualTo("testuser");
        assertThat(entity.getArea()).isNotNull();
    }

    @Test
    void updateEntityFromDto_updatesFieldsCorrectly() {
        GeoFence entity = new GeoFence("Old Name", samplePolygon(), "old@email.com", "olduser");
        GeoFenceDto dto = new GeoFenceDto("New Name", sampleCoordinates(), "new@email.com", "newuser");

        mapper.updateEntityFromDto(dto, entity);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getEmail()).isEqualTo("new@email.com");
        assertThat(entity.getUsername()).isEqualTo("newuser");
    }

    @Test
    void toDto_returnsNullCoordinates_whenAreaIsNull() {
        GeoFence entity = new GeoFence("Test Fence", null, "test@email.com", "testuser");

        GeoFenceDto dto = mapper.toDto(entity);

        assertThat(dto.getCoordinates()).isNull();
    }

    @Test
    void toEntity_returnsNullArea_whenCoordinatesIsNull() {
        GeoFenceDto dto = new GeoFenceDto("Test Fence", null, "test@email.com", "testuser");

        GeoFence entity = mapper.toEntity(dto);

        assertThat(entity.getArea()).isNull();
    }
}