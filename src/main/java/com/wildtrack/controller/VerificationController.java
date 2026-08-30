
package com.wildtrack.controller;

import com.wildtrack.dto.VerificationRequest;
import com.wildtrack.model.ApiResponse;
import com.wildtrack.service.VerifiedActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Tag(name = "Email verification", description = "Confirms ownership of an email address before acting on a request")
public class VerificationController {

    private final VerifiedActionService verifiedActionService;

    @Operation(summary = "Confirm a geo-fence with the code sent by email")
    @PostMapping("/geoFence")
    public ResponseEntity<ApiResponse> verifyGeoFence(@Valid @RequestBody VerificationRequest request) {
        return ResponseEntity.ok(new ApiResponse(
                verifiedActionService.completeGeoFence(request.email(), request.code())));
    }

    @Operation(summary = "Confirm the alert demo with the code sent by email")
    @PostMapping("/demo")
    public ResponseEntity<ApiResponse> verifyDemo(@Valid @RequestBody VerificationRequest request) {
        return ResponseEntity.ok(new ApiResponse(
                verifiedActionService.completeDemo(request.email(), request.code())));
    }
}