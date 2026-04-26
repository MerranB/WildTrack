package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.service.MovebankEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;

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
    public String updateDatabase(@PathVariable Long id) throws IOException {
        return movebankEventService.updateDatabase(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovebankEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(movebankEventService.findById(id));
    }
}
