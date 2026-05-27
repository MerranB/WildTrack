package com.wildtrack.demo;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import com.wildtrack.scheduler.GeoFenceScheduler;
import com.wildtrack.service.GeoFenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DemoService {

    private final GeoFenceService geoFenceService;
    private final MovebankEventRepository movebankEventRepository;
    private final MovebankEventMapper movebankEventMapper;
    private final GeoFenceScheduler geoFenceScheduler;

    String testGeoFenceDemo(GeoFenceDto dto){

        GeoFenceDto savedFence = geoFenceService.create(dto);
        savedFence.setLastAlertSent(LocalDateTime.now().minusDays(4));
        geoFenceService.update(savedFence.getId(), savedFence);
        List<CoordinateDto> cords = dto.getCoordinates();

        double latAvg = 0;
        double lonAvg = 0;
        for(CoordinateDto cord: cords){
            latAvg = latAvg + cord.lat();
            lonAvg = lonAvg + cord.lon();
        }
        MovebankEventDto demoItem = new MovebankEventDto(LocalDateTime.now(),
                latAvg/(cords.size()), lonAvg/(cords.size()),
                "12345678", "12345678");
        MovebankEvent demoData = movebankEventRepository.save(movebankEventMapper.toEntity(demoItem));
        geoFenceScheduler.geoFenceDemo(savedFence.getId(), demoData);
        return "Your geofence has been successfully setup! An email alert will be sent to your email:  " +
                dto.getEmail() + " in 2 minutes.";
    }
}