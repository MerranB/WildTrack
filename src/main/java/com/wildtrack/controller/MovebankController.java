package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.model.ApiResponse;
import com.wildtrack.service.MovebankEventService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import com.wildtrack.dto.BoundingBoxRequest;
import com.wildtrack.dto.CoordinateRangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Movebank Events", description = "Query and manage wildlife telemetry events ingested from the Movebank API. Data covers Magnificent" +
        "Frigatebird GPS tracking in the British Virgin Islands, 2014–2016.")
public class MovebankController {

    private final MovebankEventService movebankEventService;

    @Operation(summary = "Get all telemetry events", description = "Returns all wildlife telemetry events in the database, paginated.")
    @GetMapping("/all")
    public ResponseEntity<List<MovebankEventDto>> getAll() {
        return ResponseEntity.ok(movebankEventService.findAll());
    }

    @Operation(summary = "Trigger manual data ingestion", description = "Manually triggers ingestion of the Magnificent Frigatebird dataset from " +
            "Movebank.")
    @PostMapping("/updateDatabase")
    public ResponseEntity<ApiResponse> updateDatabase() {
        String result = movebankEventService.updateDatabase();
        if(result.equals("FULL_SUCCESS")) {
            return ResponseEntity.ok(new ApiResponse(result));
        }
        else if(result.equals("PARTIAL_SUCCESS")) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(new ApiResponse(result));
        }
        else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(result));
        }
    }

    @Operation(summary = "Get telemetry event by ID", description = "Returns a single telemetry event by its database ID.")
    @GetMapping("/{id}")
    public ResponseEntity<MovebankEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(movebankEventService.findById(id));
    }

    @Operation(summary = "Query events by bounding box", description = "Returns all telemetry events within a geographic bounding box defined by " +
            "min/max latitude and longitude. Uses a PostGIS spatial query.")
    @Parameter(name = "minLat", example = "18.3")
    @Parameter(name = "maxLat", example = "18.6")
    @Parameter(name = "minLon", example = "-64.8")
    @Parameter(name = "maxLon", example = "-64.4")

    @GetMapping("/allDataPointsByBox")
    public ResponseEntity<Page<MovebankEventDto>> allDataPointsByBox(@Valid @ModelAttribute BoundingBoxRequest request,
        @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(movebankEventService.allDataPointsByBox(request.minLon(), request.minLat(), request.maxLon(), request.maxLat(), pageable));
    }

    @Operation(summary = "Query events by radius", description = "Returns all telemetry events within a given radius (in degrees) of a coordinate" +
            "point. Uses a PostGIS spatial query. 1 degree is approximately 111km.")
    @Parameter(name = "lat", example = "18.4312")
    @Parameter(name = "lon", example = "-64.6235")
    @Parameter(name = "range", example = "1.0")

    @GetMapping("/allDataPointsByRange")
    public ResponseEntity<Page<MovebankEventDto>> allDataPointsByRange(@Valid @ModelAttribute CoordinateRangeRequest request,
        @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(movebankEventService.allDataPointsByRange(request.lat(), request.lon(), request.range(), pageable));
    }
}
