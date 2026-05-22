package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wildtrack.service.NaturalLanguageQueryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Tag(name = "Natural Language Query", description = "AI-powered natural language interface for querying wildlife telemetry data")
@Validated
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class NaturalLanguageQueryController {

    private final NaturalLanguageQueryService naturalLanguageQueryService;

    @Operation(
        summary = "Query wildlife data using natural language",
        description = """
            Accepts a plain-English query, uses Claude Haiku 4.5 to extract spatial and temporal \
            parameters, and returns matching telemetry events from the PostGIS database.

            **Dataset scope:** Magnificent Frigatebird GPS tracking, British Virgin Islands and \
            surrounding Caribbean waters, 2014–2016. Queries outside this scope will still run \
            but confidence will be low and results may be sparse.

            Select an example query from the dropdown below to get started.
            """
    )
    @GetMapping("/query")
    public ResponseEntity<Page<MovebankEventDto>> processNaturalLanguageQuery(
            @Parameter(
                description = "A plain-English location and/or time query. The dataset covers Magnificent Frigatebird tracking near the British Virgin Islands, 2014–2016.",
                examples = {
                    @ExampleObject(name = "By location and year",     value = "Show me all sightings near the British Virgin Islands in 2015"),
                    @ExampleObject(name = "By region after a date",   value = "Find tracking events near Puerto Rico after June 2014"),
                    @ExampleObject(name = "By season",                value = "Where were the frigatebirds in the Caribbean in early 2016?"),
                    @ExampleObject(name = "By date range",            value = "Show activity near Tortola between 2014 and 2015")
                }
            )
            @RequestParam @NotBlank String userPrompt,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(naturalLanguageQueryService.processNaturalLanguageQuery(userPrompt, pageable));
    }
}