package com.wildtrack.controller;

import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.model.ApiResponse;
import com.wildtrack.model.VerificationPurpose;
import com.wildtrack.service.EmailVerificationService;
import com.wildtrack.service.GeoFenceService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/geoFence")
@RequiredArgsConstructor
@Tag(name = "Geo-fence", description = """
      Create and manage geo-fences for wildlife tracking alerts. \
      Note: the scheduled alert trigger is disabled in this environment as the current dataset \
      is historical and will never produce new movement events. Use the Demo endpoint to see the \
      geo-fence alert feature in action with simulated data.
      """)
public class GeoFenceController {

    private final GeoFenceService geoFenceService;
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "Retrieves all geo-fences"
    )
    @GetMapping
    public ResponseEntity<Page<GeoFenceDto>> getAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(geoFenceService.findAll(pageable));
    }

    @Operation(
            summary = "Returns a geo-fence by ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<GeoFenceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(geoFenceService.findById(id));
    }

    @Operation(summary = "Create a geo-fence")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Tortola, BVI",
                                    value = """
                      {
                        "name": "Tortola Zone",
                        "coordinates": [
                          {"lat": 18.3812, "lon": -64.6735},
                          {"lat": 18.4812, "lon": -64.6735},
                          {"lat": 18.4812, "lon": -64.5735},
                          {"lat": 18.3812, "lon": -64.5735},
                          {"lat": 18.3812, "lon": -64.6735}
                        ],
                        "email": "researcher@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            ),
                            @ExampleObject(
                                    name = "Virgin Gorda, BVI",
                                    value = """
                      {
                        "name": "Virgin Gorda Zone",
                        "coordinates": [
                          {"lat": 18.4485, "lon": -64.4610},
                          {"lat": 18.5485, "lon": -64.4610},
                          {"lat": 18.5485, "lon": -64.3610},
                          {"lat": 18.4485, "lon": -64.3610},
                          {"lat": 18.4485, "lon": -64.4610}
                        ],
                        "email": "researcher@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            ),
                            @ExampleObject(
                                    name = "St. Thomas, USVI",
                                    value = """
                      {
                        "name": "St. Thomas Zone",
                        "coordinates": [
                          {"lat": 18.2919, "lon": -64.9807},
                          {"lat": 18.3919, "lon": -64.9807},
                          {"lat": 18.3919, "lon": -64.8807},
                          {"lat": 18.2919, "lon": -64.8807},
                          {"lat": 18.2919, "lon": -64.9807}
                        ],
                        "email": "researcher@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            ),
                            @ExampleObject(
                                    name = "Anegada, BVI",
                                    value = """
                      {
                        "name": "Anegada Zone",
                        "coordinates": [
                          {"lat": 18.6772, "lon": -64.3833},
                          {"lat": 18.7772, "lon": -64.3833},
                          {"lat": 18.7772, "lon": -64.2833},
                          {"lat": 18.6772, "lon": -64.2833},
                          {"lat": 18.6772, "lon": -64.3833}
                        ],
                        "email": "researcher@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            )
                    }
            )
    )
    @PostMapping()
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody GeoFenceDto dto) {
        emailVerificationService.startVerification(
                dto.getEmail(), VerificationPurpose.GEO_FENCE, dto);

        return ResponseEntity.accepted().body(new ApiResponse(
                "A 6 digit code has been sent to " + dto.getEmail()
                        + ". Add the code to POST /api/v1/verify/geoFence to finish creating the geo-fence."));
    }

    @Operation(
            summary = "Update a geo-fence",
            description = "Update an existing geo-fence by ID. Use the ID returned from the create call."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Tortola, BVI",
                                    value = """
                      {
                        "name": "Tortola Zone Updated",
                        "coordinates": [
                          {"lat": 18.3812, "lon": -64.6735},
                          {"lat": 18.4812, "lon": -64.6735},
                          {"lat": 18.4812, "lon": -64.5735},
                          {"lat": 18.3812, "lon": -64.5735},
                          {"lat": 18.3812, "lon": -64.6735}
                        ],
                        "email": "updated@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            ),
                            @ExampleObject(
                                    name = "Virgin Gorda, BVI",
                                    value = """
                      {
                        "name": "Virgin Gorda Zone Updated",
                        "coordinates": [
                          {"lat": 18.4485, "lon": -64.4610},
                          {"lat": 18.5485, "lon": -64.4610},
                          {"lat": 18.5485, "lon": -64.3610},
                          {"lat": 18.4485, "lon": -64.3610},
                          {"lat": 18.4485, "lon": -64.4610}
                        ],
                        "email": "updated@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            ),
                            @ExampleObject(
                                    name = "St. Thomas, USVI",
                                    value = """
                      {
                        "name": "St. Thomas Zone Updated",
                        "coordinates": [
                          {"lat": 18.2919, "lon": -64.9807},
                          {"lat": 18.3919, "lon": -64.9807},
                          {"lat": 18.3919, "lon": -64.8807},
                          {"lat": 18.2919, "lon": -64.8807},
                          {"lat": 18.2919, "lon": -64.9807}
                        ],
                        "email": "updated@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            ),
                            @ExampleObject(
                                    name = "Anegada, BVI",
                                    value = """
                      {
                        "name": "Anegada Zone Updated",
                        "coordinates": [
                          {"lat": 18.6772, "lon": -64.3833},
                          {"lat": 18.7772, "lon": -64.3833},
                          {"lat": 18.7772, "lon": -64.2833},
                          {"lat": 18.6772, "lon": -64.2833},
                          {"lat": 18.6772, "lon": -64.3833}
                        ],
                        "email": "updated@wildtrack.com",
                        "username": "researcher1",
                        "lastAnimalCount": 0
                      }
                      """
                            )
                    }
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<GeoFenceDto> update(@PathVariable Long id,
                                              @Valid @RequestBody GeoFenceDto dto) {
        return ResponseEntity.ok(geoFenceService.update(id, dto));
    }

    @Operation(
            summary = "Deletes a geo-fence by ID"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        geoFenceService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Successfully deleted " + id));
    }
}

