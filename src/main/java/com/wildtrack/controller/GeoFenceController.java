package com.wildtrack.controller;

import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.service.GeoFenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/geoFence")
@RequiredArgsConstructor
public class GeoFenceController {

    private final GeoFenceService geoFenceService;

    @GetMapping
    public ResponseEntity<Page<GeoFenceDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(geoFenceService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeoFenceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(geoFenceService.findById(id));
    }
     
    @PostMapping("/create")
    public ResponseEntity<GeoFenceDto> create(@Valid @RequestBody GeoFenceDto dto) {
        GeoFenceDto created = geoFenceService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeoFenceDto> update(@PathVariable Long id,
                                          @Valid @RequestBody GeoFenceDto dto) {
        return ResponseEntity.ok(geoFenceService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        geoFenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
