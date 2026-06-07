package com.wildtrack.service;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.exception.NaturalLanguageQueryException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.repository.MovebankEventRepository;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaturalLanguageQueryServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @Mock
    private AssistantMessage assistantMessage;

    @Mock
    private MovebankEventRepository movebankEventRepository;

    @Mock
    private MovebankEventMapper movebankEventMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private NaturalLanguageQueryService naturalLanguageQueryService;

    @Mock
    private org.springframework.ai.chat.metadata.ChatResponseMetadata chatResponseMetadata;

    @Mock
    private org.springframework.ai.chat.metadata.Usage usage;

    private void mockClaudeResponse(String responseText) {
        when(assistantMessage.getText()).thenReturn(responseText);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(usage.getPromptTokens()).thenReturn(100);
        when(chatResponseMetadata.getUsage()).thenReturn(usage);
        when(chatResponse.getMetadata()).thenReturn(chatResponseMetadata);
    }

    @Test
    void processNaturalLanguageQuery_returnsPage_whenValidResponse() {
        mockClaudeResponse("""
                {
                    "longitude": -87.6298,
                    "latitude": 41.8781,
                    "range": 5.0
                }
                """);
        when(movebankEventRepository.allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<MovebankEventDto> result = naturalLanguageQueryService.processNaturalLanguageQuery("show me animals near Chicago", Pageable.unpaged());

        assertThat(result).isNotNull();
    }

    @Test
    void processNaturalLanguageQuery_throwsException_whenClaudeReturnsError() {
        mockClaudeResponse("""
                {"error": "Could not identify a location from the query"}
                """);
        Pageable pageable = Pageable.unpaged();
        assertThrows(NaturalLanguageQueryException.class,
                () -> naturalLanguageQueryService.processNaturalLanguageQuery("asdfjkl", pageable));
    }

    private static Stream<String> invalidClaudeResponses() {
        return Stream.of(
                "{This is not valid JSON at all}",
                "{latitude: 41.8781, longitude: -87.6298}",
                "I cannot identify any location from the query",
                null
        );
    }

    @ParameterizedTest
    @MethodSource("invalidClaudeResponses")
    void processNaturalLanguageQuery_throwsException_whenResponseIsInvalid(String response) {
        mockClaudeResponse(response);
        Pageable pageable = Pageable.unpaged();
            assertThrows(NaturalLanguageQueryException.class,
                () -> naturalLanguageQueryService.processNaturalLanguageQuery("asdfjkl", pageable));
    }

    @Test
    void processNaturalLanguageQuery_queriesWithDateRange_whenYearMentioned() {
        mockClaudeResponse("""
                {
                    "latitude": 18.2,
                    "longitude": -66.5,
                    "range": 5.0,
                    "startDate": "2015-01-01",
                    "endDate": "2015-12-31"
                }
                """);
        when(movebankEventRepository.allDataPointsByRangeAndTime(anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        naturalLanguageQueryService.processNaturalLanguageQuery("show me frigatebird sightings in 2015", Pageable.unpaged());

        verify(movebankEventRepository).allDataPointsByRangeAndTime(
                anyDouble(), anyDouble(), anyDouble(),
                eq(LocalDate.of(2015, 1, 1)), eq(LocalDate.of(2015, 12, 31)),
                any(Pageable.class));
    }

    @Test
    void processNaturalLanguageQuery_queriesWithDatasetEnd_whenOnlyStartDatePresent() {
        mockClaudeResponse("""
                {
                    "latitude": 18.2,
                    "longitude": -66.5,
                    "range": 5.0,
                    "startDate": "2014-01-01"
                }
                """);
        when(movebankEventRepository.allDataPointsByRangeAndTime(anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        naturalLanguageQueryService.processNaturalLanguageQuery("show me animals after 2014", Pageable.unpaged());

        verify(movebankEventRepository).allDataPointsByRangeAndTime(
                anyDouble(), anyDouble(), anyDouble(),
                eq(LocalDate.of(2014, 1, 1)), eq(LocalDate.of(2016, 12, 31)),
                any(Pageable.class));
    }

    @Test
    void processNaturalLanguageQuery_queriesWithDatasetStart_whenOnlyEndDatePresent() {
        mockClaudeResponse("""
                {
                    "latitude": 18.2,
                    "longitude": -66.5,
                    "range": 5.0,
                    "endDate": "2016-12-31"
                }
                """);
        when(movebankEventRepository.allDataPointsByRangeAndTime(anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        naturalLanguageQueryService.processNaturalLanguageQuery("show me animals before 2016", Pageable.unpaged());

        verify(movebankEventRepository).allDataPointsByRangeAndTime(
                anyDouble(), anyDouble(), anyDouble(),
                eq(LocalDate.of(2014, 1, 1)), eq(LocalDate.of(2016, 12, 31)),
                any(Pageable.class));
    }

    @Test
    void processNaturalLanguageQuery_queriesWithoutDates_whenNoTimeMentioned() {
        mockClaudeResponse("""
                {
                    "latitude": 41.8781,
                    "longitude": -87.6298,
                    "range": 5.0
                }
                """);
        when(movebankEventRepository.allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(Page.empty());

        naturalLanguageQueryService.processNaturalLanguageQuery("show me animals near Chicago", Pageable.unpaged());

        verify(movebankEventRepository).allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class));
    }

    @Test
    void processNaturalLanguageQuery_forwardsPaginationToRepository() {
        mockClaudeResponse("""
                {
                    "longitude": -87.6298,
                    "latitude": 41.8781,
                    "range": 5.0
                }
                """);
        Pageable pageable = PageRequest.of(0, 5);
        when(movebankEventRepository.allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(Page.empty());

        naturalLanguageQueryService.processNaturalLanguageQuery("show me animals near Chicago", pageable);

        verify(movebankEventRepository).allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), eq(pageable));
    }

    @Test
    void processNaturalLanguageQuery_queriesWithBothDates_whenCombinedSpatialAndDateQuery() {
        mockClaudeResponse("""
                {
                    "longitude": -66.5,
                    "latitude": 18.2,
                    "range": 5.0,
                    "startDate": "2014-01-01",
                    "endDate": "2014-12-31"
                }
                """);
        when(movebankEventRepository.allDataPointsByRangeAndTime(anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        naturalLanguageQueryService.processNaturalLanguageQuery("show me animals near the Caribbean in 2014", Pageable.unpaged());

        verify(movebankEventRepository).allDataPointsByRangeAndTime(
                anyDouble(), anyDouble(), anyDouble(),
                eq(LocalDate.of(2014, 1, 1)), eq(LocalDate.of(2014, 12, 31)),
                any(Pageable.class));
    }
}
