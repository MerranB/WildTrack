package com.wildtrack.demo;

import com.wildtrack.dto.GeoFenceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    @PostMapping
    public ResponseEntity<String> testGeoFenceDemo(@Valid @RequestBody GeoFenceDto dto) {
        return ResponseEntity.ok(demoService.testGeoFenceDemo(dto));
    }
}
