package com.wildtrack.demo;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import com.wildtrack.scheduler.GeoFenceScheduler;
import com.wildtrack.service.GeoFenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DemoServiceTest {

    @Mock private GeoFenceService geoFenceService;
    @Mock private MovebankEventRepository movebankEventRepository;
    @Mock private MovebankEventMapper movebankEventMapper;
    @Mock private GeoFenceScheduler geoFenceScheduler;

    @InjectMocks
    private DemoService demoService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoFenceDto validDto() {
        return new GeoFenceDto("Test Fence",
                List.of(new CoordinateDto(1.0, 1.0), new CoordinateDto(1.0, -1.0),
                        new CoordinateDto(-1.0, -1.0), new CoordinateDto(-1.0, 1.0),
                        new CoordinateDto(1.0, 1.0)),
                "test@example.com", "testuser", 0);
    }

    private void setupMocks(Long fenceId) {
        GeoFenceDto savedDto = new GeoFenceDto("Test Fence", List.of(), "test@example.com", "testuser", 0);
        savedDto.setId(fenceId);
        when(geoFenceService.create(any())).thenReturn(savedDto);
        when(geoFenceService.update(any(), any())).thenReturn(savedDto);
        MovebankEvent entity = new MovebankEvent(LocalDateTime.now(),
                geometryFactory.createPoint(new Coordinate(0.0, 0.0)), "12345678", "12345678");
        when(movebankEventMapper.toEntity(any())).thenReturn(entity);
        when(movebankEventRepository.save(any())).thenReturn(entity);
    }

    @Test
    void runDemo_createsGeofenceAndTriggersScheduler() {
        setupMocks(1L);

        demoService.runDemo(validDto());

        verify(geoFenceService).create(any());
        verify(geoFenceService).update(eq(1L), any());
        verify(movebankEventRepository).save(any());
        verify(geoFenceScheduler).geoFenceDemo(eq(1L), any(MovebankEvent.class));
    }

    @Test
    void runDemo_savesEventAtCentroid() {
        setupMocks(1L);
        ArgumentCaptor<MovebankEventDto> captor = ArgumentCaptor.forClass(MovebankEventDto.class);

        demoService.runDemo(validDto());

        verify(movebankEventMapper).toEntity(captor.capture());
        MovebankEventDto captured = captor.getValue();
        assertThat(captured.getLocationLat()).isCloseTo(0.2, within(1e-9));
        assertThat(captured.getLocationLong()).isCloseTo(0.2, within(1e-9));
    }
}