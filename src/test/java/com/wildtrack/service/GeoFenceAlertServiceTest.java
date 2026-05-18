package com.wildtrack.service;

import com.wildtrack.email.EmailDetail;
import com.wildtrack.email.EmailServiceImpl;
import com.wildtrack.model.GeoFence;
import com.wildtrack.repository.GeoFenceRepository;
import com.wildtrack.repository.MovebankEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GeoFenceAlertServiceTest {

    @Mock private GeoFenceRepository geoFenceRepository;
    @Mock private MovebankEventRepository movebankEventRepository;
    @Mock private EmailServiceImpl emailServiceImpl;

    @InjectMocks
    private GeoFenceAlertService geoFenceAlertService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Polygon samplePolygon() {
        Coordinate[] coords = {
                new Coordinate(1.0, 1.0), new Coordinate(-1.0, 1.0),
                new Coordinate(-1.0, -1.0), new Coordinate(1.0, -1.0),
                new Coordinate(1.0, 1.0)
        };
        return geometryFactory.createPolygon(coords);
    }

    private GeoFence fenceWithCount(int count) {
        GeoFence fence = new GeoFence("Test Fence", samplePolygon(), "test@example.com", "testuser", count);
        fence.setLastAlertSent(LocalDateTime.now().minusDays(4));
        return fence;
    }

    @Test
    void checkGeoFences_sendsEmail_whenCountIncreases() {
        when(geoFenceRepository.findAll()).thenReturn(List.of(fenceWithCount(0)));
        when(movebankEventRepository.countAnimalsInGeofence(any())).thenReturn(1);

        geoFenceAlertService.checkGeoFences();

        verify(emailServiceImpl).sendSimpleMail(any(EmailDetail.class));
    }

    @Test
    void checkGeoFences_sendsEmail_whenCountDecreases() {
        when(geoFenceRepository.findAll()).thenReturn(List.of(fenceWithCount(2)));
        when(movebankEventRepository.countAnimalsInGeofence(any())).thenReturn(1);

        geoFenceAlertService.checkGeoFences();

        verify(emailServiceImpl).sendSimpleMail(any(EmailDetail.class));
    }

    @Test
    void checkGeoFences_doesNotSendEmail_whenCountUnchanged() {
        when(geoFenceRepository.findAll()).thenReturn(List.of(fenceWithCount(1)));
        when(movebankEventRepository.countAnimalsInGeofence(any())).thenReturn(1);

        geoFenceAlertService.checkGeoFences();

        verify(emailServiceImpl, never()).sendSimpleMail(any());
    }

    @Test
    void checkGeoFences_doesNotSendEmail_whenCooldownNotExpired() {
        GeoFence fence = new GeoFence("Test Fence", samplePolygon(), "test@example.com",
                "testuser", 0);
        when(geoFenceRepository.findAll()).thenReturn(List.of(fence));
        when(movebankEventRepository.countAnimalsInGeofence(any())).thenReturn(1);

        geoFenceAlertService.checkGeoFences();

        verify(emailServiceImpl, never()).sendSimpleMail(any());
    }

    @Test
    void checkGeoFences_updatesLastAnimalCountAndSaves_afterAlert() {
        GeoFence fence = fenceWithCount(0);
        when(geoFenceRepository.findAll()).thenReturn(List.of(fence));
        when(movebankEventRepository.countAnimalsInGeofence(any())).thenReturn(1);

        geoFenceAlertService.checkGeoFences();

        assertThat(fence.getLastAnimalCount()).isEqualTo(1);
        verify(geoFenceRepository).save(fence);
    }
}