package com.wildtrack.service;

import com.wildtrack.email.EmailDetail;
import com.wildtrack.email.EmailServiceImpl;
import com.wildtrack.model.GeoFence;
import com.wildtrack.repository.GeoFenceRepository;
import com.wildtrack.repository.MovebankEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class GeoFenceAlertService {

    private final GeoFenceRepository geoFenceRepository;
    private final MovebankEventRepository movebankEventRepository;
    private final EmailServiceImpl emailServiceImpl;
    private static final Logger log = LoggerFactory.getLogger(GeoFenceAlertService.class);


    public void checkGeoFences() {
        List<GeoFence> allGeoFences = geoFenceRepository.findAll();
        for(GeoFence geofence: allGeoFences){
            int currentAnimalCount = movebankEventRepository.countAnimalsInGeofence(geofence.getArea());
            if(currentAnimalCount != geofence.getLastAnimalCount() && 
            geofence.getLastAlertSent().isBefore(LocalDateTime.now().minusDays(3))){

                emailServiceImpl.sendSimpleMail( new EmailDetail(geofence.getEmail(),
                geofence.getName() + " animal count has changed to " + currentAnimalCount,
                "GeoFence " + geofence.getName() + " has been triggered!",
                null));
                
                geofence.setLastAnimalCount(currentAnimalCount);
                geofence.setLastAlertSent(LocalDateTime.now());
                geoFenceRepository.save(geofence);
                log.info("Email sent");
            }
        }
    }
}