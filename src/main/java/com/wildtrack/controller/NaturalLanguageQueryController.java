package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wildtrack.service.NaturalLanguageQueryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class NaturalLanguageQueryController {

    private final NaturalLanguageQueryService naturalLanguageQueryService;

    @GetMapping("/query")
    public ResponseEntity<Page<MovebankEventDto>> processNaturalLanguageQuery(@RequestParam @NotBlank String userPrompt, Pageable pageable) {
        return ResponseEntity.ok(naturalLanguageQueryService.processNaturalLanguageQuery(userPrompt, pageable));
    }
}