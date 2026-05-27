package com.wildtrack.service;

import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.GeoFenceMapper;
import com.wildtrack.model.GeoFence;
import com.wildtrack.repository.GeoFenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeoFenceServiceTest {

    @Mock
    private GeoFenceRepository geoFenceRepository;

    @Mock
    private GeoFenceMapper geoFenceMapper;

    @InjectMocks
    private GeoFenceService geoFenceService;

    private Polygon samplePolygon() {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Coordinate[] coords = {
                new Coordinate(20.0, 10.0),
                new Coordinate(21.0, 10.0),
                new Coordinate(21.0, 11.0),
                new Coordinate(20.0, 11.0),
                new Coordinate(20.0, 10.0)
        };
        return factory.createPolygon(coords);
    }

    private GeoFence sampleEntity() {
        return new GeoFence("Test Fence", samplePolygon(), "test@email.com", "testuser",
                0);
    }

    private GeoFenceDto sampleDto() {
        return new GeoFenceDto(
                "Test Fence",
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(11.0, 20.0),
                        new CoordinateDto(10.0, 20.0)
                ),
                "test@email.com",
                "testuser",
                0
        );
    }

    private GeoFenceDto sampleDto_polygon_malformed() {
        return new GeoFenceDto(
                "Test Fence",
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(11.0, 20.0)
                ),
                "test@email.com",
                "testuser",
                0
        );
    }

    @Test
    void findAll_returnsPageOfDtos() {
        Page<GeoFence> page = new PageImpl<>(List.of(sampleEntity()));
        when(geoFenceRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(geoFenceMapper.toDto(any())).thenReturn(sampleDto());

        Page<GeoFenceDto> result = geoFenceService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Test Fence");
    }

    @Test
    void findById_returnsCorrectDto() {
        when(geoFenceRepository.findById(1L)).thenReturn(Optional.of(sampleEntity()));
        when(geoFenceMapper.toDto(any())).thenReturn(sampleDto());

        GeoFenceDto result = geoFenceService.findById(1L);

        assertThat(result.getName()).isEqualTo("Test Fence");
        assertThat(result.getEmail()).isEqualTo("test@email.com");
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(geoFenceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> geoFenceService.findById(99L));
    }

    @Test
    void create_savesAndReturnsDto() {
        when(geoFenceMapper.toEntity(any())).thenReturn(sampleEntity());
        when(geoFenceRepository.save(any())).thenReturn(sampleEntity());
        when(geoFenceMapper.toDto(any())).thenReturn(sampleDto());

        GeoFenceDto result = geoFenceService.create(sampleDto());

        verify(geoFenceRepository).save(any(GeoFence.class));
        assertThat(result.getName()).isEqualTo("Test Fence");
    }

    @Test
    void update_updatesAndReturnsDto() {
        when(geoFenceRepository.findById(1L)).thenReturn(Optional.of(sampleEntity()));
        when(geoFenceMapper.toDto(any())).thenReturn(sampleDto());

        GeoFenceDto result = geoFenceService.update(1L, sampleDto());

        verify(geoFenceMapper).updateEntityFromDto(any(), any());
        assertThat(result.getName()).isEqualTo("Test Fence");
    }

    @Test
    void update_throwsWhenNotFound() {
        when(geoFenceRepository.findById(99L)).thenReturn(Optional.empty());
        GeoFenceDto dto = sampleDto();
        assertThrows(ResourceNotFoundException.class, () -> geoFenceService.update(99L, dto));
    }

    @Test
    void delete_deletesSuccessfully() {
        when(geoFenceRepository.existsById(1L)).thenReturn(true);

        geoFenceService.delete(1L);

        verify(geoFenceRepository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(geoFenceRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> geoFenceService.delete(99L));
    }

    @Test
    void create_throwsIllegalArgument_whenPolygonNotClosed() {
        GeoFenceDto dto = sampleDto_polygon_malformed();
        assertThrows(IllegalArgumentException.class, () -> geoFenceService.create(dto));
    }

    @Test
    void update_throwsIllegalArgument_whenPolygonNotClosed() {
        GeoFenceDto dto = sampleDto_polygon_malformed();
        assertThrows(IllegalArgumentException.class, () ->  geoFenceService.update(99L,dto ));
    }
}