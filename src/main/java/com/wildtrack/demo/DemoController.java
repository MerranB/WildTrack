package com.wildtrack.demo;

import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.model.ApiResponse;
import com.wildtrack.model.VerificationPurpose;
import com.wildtrack.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
@Tag(name = "Demo", description = "Demonstrates the geo-fence alert feature with simulated data")
public class DemoController {

    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "Trigger a geo-fence alert demo",
            description = """
           Demonstrates the full geo-fence alert pipeline using the production code path
 
                    When triggered, this endpoint:
                    1. Creates a temporary geo-fence using the coordinates you provide
                    2. Inserts a real animal tracking event into the database inside the geo-fence boundary
                    3. Schedules the production geo-fence scheduler to run in 2 minutes.
                    4. The scheduler performs a real PostGIS spatial query, detects that the animal count inside \\
                    the geo-fence has changed, and fires a real alert email to the address you provide
                    5. The temporary geo-fence and simulated tracking event are automatically cleaned up after the check completes

                    Note: the lastAnimalCount field represents what the system believes is currently in the geo-fence.
                               Regardless of what value you provide, the actual count in the database will be 0 for this area.
                               The demo inserts exactly 1 tracking event inside your geo-fence boundary, bringing the real count
                               to 1. This mismatch is what triggers the alert.

                    **Important:** You must enter a real email address. An actual alert email will be sent to whatever \\
                    address you provide. If you use a fake email the demo will run but you will not receive the alert."""
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Tortola, BVI",
                                    value = """
                      {
                        "name": "Demo Tortola Zone",
                        "coordinates": [
                          {"lat": 18.3812, "lon": -64.6735},
                          {"lat": 18.4812, "lon": -64.6735},
                          {"lat": 18.4812, "lon": -64.5735},
                          {"lat": 18.3812, "lon": -64.5735},
                          {"lat": 18.3812, "lon": -64.6735}
                        ],
                        "email": "your-real-email@example.com",
                        "username": "demo-user",
                        "lastAnimalCount": 0
                      }
                      """
                            )
                    }
            )
    )
    @PostMapping
    public ResponseEntity<ApiResponse> testGeoFenceDemo(@Valid @RequestBody GeoFenceDto dto) {
        emailVerificationService.startVerification(
                dto.getEmail(), VerificationPurpose.DEMO, dto);

        return ResponseEntity.accepted().body(new ApiResponse(
                "A 6 digit code has been sent to " + dto.getEmail()
                        + ". Add the code to POST /api/v1/verify/demo to run the alert demo."));
    }
}
