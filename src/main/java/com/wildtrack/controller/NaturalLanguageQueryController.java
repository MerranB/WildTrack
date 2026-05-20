package com.wildtrack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wildtrack.service.NaturalLanguageQueryService;
import com.wildtrack.analysis.SpatialQueryParams;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class NaturalLanguageQueryController {

    private final NaturalLanguageQueryService naturalLanguageQueryService;

    @GetMapping
    public ResponseEntity<SpatialQueryParams> processNaturalLanguageQuery(@RequestParam @NotBlank String userPrompt) {
        return ResponseEntity.ok(naturalLanguageQueryService.processNaturalLanguageQuery(userPrompt));
    }
}