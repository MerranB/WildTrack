package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.service.MovebankEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import com.wildtrack.dto.BoundingBoxRequest;
import com.wildtrack.dto.CoordinateRangeRequest;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class MovebankController {

    private final MovebankEventService movebankEventService;

    @GetMapping
    public ResponseEntity<Page<MovebankEventDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(movebankEventService.findAll(pageable));
    }

    @PostMapping("/updateDatabase/{id}")
    public ResponseEntity<String> updateDatabase(@PathVariable Long id) {
        String result = movebankEventService.updateDatabase(id);
        if(result.equals("FULL_SUCCESS")) {
            return ResponseEntity.ok(result);
        }
        else if(result.equals("PARTIAL_SUCCESS")) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        }
        else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovebankEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(movebankEventService.findById(id));
    }

    @GetMapping("/allDataPointsByBox")
    public ResponseEntity<Page<MovebankEventDto>> allDataPointsByBox(@Valid @ModelAttribute BoundingBoxRequest request,
        Pageable pageable) {
        return ResponseEntity.ok(movebankEventService.allDataPointsByBox(request.minLon(), request.minLat(), request.maxLon(), request.maxLat(), pageable));
    }

    @GetMapping("/allDataPointsByRange")
    public ResponseEntity<Page<MovebankEventDto>> allDataPointsByRange(@Valid @ModelAttribute CoordinateRangeRequest request,
        Pageable pageable) {
        return ResponseEntity.ok(movebankEventService.allDataPointsByRange(request.lat(), request.lon(), request.range(), pageable));
    }
}
