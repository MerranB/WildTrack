package com.wildtrack.service;

import com.wildtrack.email.EmailDetail;
import com.wildtrack.email.EmailService;
import com.wildtrack.model.GeoFence;
import com.wildtrack.repository.GeoFenceRepository;
import com.wildtrack.repository.MovebankEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class GeoFenceAlertService {

    private final GeoFenceRepository geoFenceRepository;
    private final MovebankEventRepository movebankEventRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(GeoFenceAlertService.class);


    public void checkGeoFences() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<GeoFence> page;
        do {
            page = geoFenceRepository.findAll(pageable);
            for (GeoFence geofence : page.getContent()) {
                int currentAnimalCount = movebankEventRepository.countAnimalsInGeofence(geofence.getArea());
                if(currentAnimalCount != geofence.getLastAnimalCount() &&
                geofence.getLastAlertSent().isBefore(LocalDateTime.now().minusDays(3))){

                    emailService.sendSimpleMail( new EmailDetail(geofence.getEmail(),
                    geofence.getName() + " animal count has changed from " + geofence.getLastAnimalCount() +
                            " to " + currentAnimalCount,
                    "GeoFence " + geofence.getName() + " has been triggered!",
                    null));

                    geofence.setLastAnimalCount(currentAnimalCount);
                    geofence.setLastAlertSent(LocalDateTime.now());
                    geoFenceRepository.save(geofence);
                    log.info("Alert sent for geo-fence: {}", geofence.getName());
                }
            }
            pageable = pageable.next();
        } while (page.hasNext());
    }
}