package com.wildtrack.service;

import com.wildtrack.model.MovebankEvent;
import com.wildtrack.util.SanitizationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDate;
import java.util.List;
import com.wildtrack.analysis.SpatialQueryParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.exception.NaturalLanguageQueryException;
import com.wildtrack.repository.MovebankEventRepository;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.client.dto.MovebankEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RequiredArgsConstructor
@Service
public class NaturalLanguageQueryService {

    private final MovebankEventRepository movebankEventRepository;
    private final MovebankEventMapper movebankEventMapper;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(NaturalLanguageQueryService.class);
    private final ChatModel chatModel;
    // Package-private so tests can assert the fill behaviour against the bounds themselves
    // rather than hardcoded literals that break every time these values are tuned.
    static final LocalDate DATASET_START = LocalDate.of(1980, 1, 1);
    static final LocalDate DATASET_END = LocalDate.of(2026, 7, 31);
    private static final String SYSTEM_PROMPT = """
    You are a wildlife tracking assistant that extracts spatial parameters from natural language queries.
    
    Given a user query, extract the following information and return ONLY a JSON object with no additional text, explanation, or markdown formatting:
    
    {
        "latitude": <decimal number, required>,
        "longitude": <decimal number, required>,
        "range": <decimal number in degrees, required, max 10>,
        "startDate": "<ISO date string YYYY-MM-DD, optional>",
        "endDate": "<ISO date string YYYY-MM-DD, optional>"
    }
    
    Rules:
    - latitude must be between -90 and 90
    - longitude must be between -180 and 180
    - range must be between 0 and 10 (in degrees, where 1 degree is approximately 111km)
    - If you cannot identify any location at all, return {"error": "Could not identify a location from the query"}
    - Never return anything other than the JSON object
    - Do not include markdown code blocks or any other formatting
    Additional rules for time parameters:
    - startDate and endDate are optional — only include them if the query mentions a time period
    - If only a year is mentioned (e.g. "in 2015"), set startDate to YYYY-01-01 and endDate to YYYY-12-31
    - If only a start time is mentioned with no end (e.g. "after 2014"), set startDate only and omit endDate
    - If only an end time is mentioned (e.g. "before 2016"), set endDate only and omit startDate
    - Dates must be in ISO format YYYY-MM-DD
    - If no time period is mentioned, omit both fields entirely
    """;

    public Page<MovebankEventDto> processNaturalLanguageQuery(String userPrompt, Pageable pageable){
        userPrompt = SanitizationUtils.sanitizeUserPrompt(userPrompt);

        Prompt prompt = new Prompt(List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(userPrompt)
        ));

        ChatResponse response = chatModel.call(prompt);

        log.info("Tokens used - input: {}, output: {}",
                response.getMetadata().getUsage().getPromptTokens(),
                response.getMetadata().getUsage().getCompletionTokens());

        String result = response.getResult().getOutput().getText();

        if(result!= null && result.contains("{") && result.contains("}")) {
            result = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
        }
        else{
            log.error("Claude failed to response correctly, responded with {}", result);
            throw new NaturalLanguageQueryException("AI failed to process result correctly.");
        }

        if(result.contains("\"error\":")){
            throw new NaturalLanguageQueryException("Could not process user's request.");
        }

        try{
            return fetchResults(objectMapper.readValue(result, SpatialQueryParams.class), pageable).map(movebankEventMapper::toDto);
        }
        catch(Exception e){
            log.error(e.toString());
            throw new NaturalLanguageQueryException("Error processing the json.");
        }
    }

    private Page<MovebankEvent> fetchResults (SpatialQueryParams spatialQueryParams, Pageable pageable){
        if(spatialQueryParams.startDate() != null && spatialQueryParams.endDate() != null){
            return movebankEventRepository.allDataPointsByRangeAndTime(spatialQueryParams.latitude(), spatialQueryParams.longitude(),
                    spatialQueryParams.range(), spatialQueryParams.startDate(), spatialQueryParams.endDate(), pageable);
        }
        else if(spatialQueryParams.startDate() != null){
            return movebankEventRepository.allDataPointsByRangeAndTime(spatialQueryParams.latitude(), spatialQueryParams.longitude(),
                    spatialQueryParams.range(), spatialQueryParams.startDate(), DATASET_END, pageable);
        }
        else if(spatialQueryParams.endDate() != null){
            return movebankEventRepository.allDataPointsByRangeAndTime(spatialQueryParams.latitude(), spatialQueryParams.longitude(),
                    spatialQueryParams.range(), DATASET_START, spatialQueryParams.endDate(), pageable);
        }
        return movebankEventRepository.allDataPointsByRange(spatialQueryParams.latitude(), spatialQueryParams.longitude(),
                    spatialQueryParams.range(), pageable);

    }
}