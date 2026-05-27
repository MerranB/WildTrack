package com.wildtrack.scheduler;

import com.wildtrack.model.MovebankEvent;
import com.wildtrack.service.GeoFenceService;
import com.wildtrack.service.MovebankEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.wildtrack.service.GeoFenceAlertService;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class GeoFenceScheduler {

    private final GeoFenceAlertService geoFenceAlertService;
    private static final Logger log = LoggerFactory.getLogger(GeoFenceScheduler.class);
    private final TaskScheduler taskScheduler;
    private final GeoFenceService geoFenceService;
    private final MovebankEventService movebankEventService;

    @Scheduled(cron = "${scheduler.cronTime.geoFence}")
    public void geoFenceChecker() {
       geoFenceAlertService.checkGeoFences();
    }

    public void geoFenceDemo(long geoFenceID, MovebankEvent demoData){
        taskScheduler.schedule(() -> {
            geoFenceAlertService.checkGeoFences();
            try {
                geoFenceService.delete(geoFenceID);
            }
            finally {
                movebankEventService.delete(demoData.getId());
            }
                },
            Instant.now().plusSeconds(120));
    }
}