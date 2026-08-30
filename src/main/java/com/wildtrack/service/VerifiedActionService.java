package com.wildtrack.service;

import com.wildtrack.demo.DemoService;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.model.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerifiedActionService {

    private final EmailVerificationService emailVerificationService;
    private final GeoFenceService geoFenceService;
    private final DemoService demoService;

    public String completeGeoFence(String email, String code) {
        GeoFenceDto pending = emailVerificationService.consume(
                email, code, VerificationPurpose.GEO_FENCE, GeoFenceDto.class);

        GeoFenceDto created = geoFenceService.create(pending);
        return "Email confirmed. Geo-fence " + created.getId() + " has been created.";
    }

    public String completeDemo(String email, String code) {
        GeoFenceDto pending = emailVerificationService.consume(
                email, code, VerificationPurpose.DEMO, GeoFenceDto.class);

        return "Email confirmed. " + demoService.runDemo(pending);
    }
}