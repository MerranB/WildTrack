package com.wildtrack.scheduler;

import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import com.wildtrack.service.GeoFenceAlertService;
import com.wildtrack.service.GeoFenceService;
import com.wildtrack.service.MovebankEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeoFenceSchedulerTest {

    @Mock private GeoFenceAlertService geoFenceAlertService;
    @Mock private TaskScheduler taskScheduler;
    @Mock private GeoFenceService geoFenceService;
    @Mock private MovebankEventService movebankEventService;

    @InjectMocks
    private GeoFenceScheduler geoFenceScheduler;

    private MovebankEvent demoEvent() {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        return new MovebankEvent(LocalDateTime.now(),
                gf.createPoint(new Coordinate(0.0, 0.0)), "12345678", "12345678");
    }

    @Test
    void geoFenceChecker_cron_readsFromProperties() throws NoSuchMethodException {
        Method method = GeoFenceScheduler.class.getMethod("geoFenceChecker");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${scheduler.cronTime.geoFence}");
    }

    @Test
    void geoFenceChecker_callsCheckGeoFences() {
        geoFenceScheduler.geoFenceChecker();

        verify(geoFenceAlertService).checkGeoFences();
    }

    @Test
    void geoFenceDemo_schedulesDelayedTask() {
        geoFenceScheduler.geoFenceDemo(1L, demoEvent());

        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void geoFenceDemo_runnable_checksAndCleansUp() {
        MovebankEvent demoData = demoEvent();
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        geoFenceScheduler.geoFenceDemo(1L, demoData);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getValue().run();

        verify(geoFenceAlertService).checkGeoFences();
        verify(geoFenceService).delete(1L);
        verify(movebankEventService).delete(demoData.getId());
    }
}