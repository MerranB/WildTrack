package com.wildtrack.service;

import com.wildtrack.analysis.SpatialQueryParams;
import com.wildtrack.exception.NaturalLanguageQueryException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NaturalLanguageQueryService naturalLanguageQueryService;

    private void mockClaudeResponse(String responseText) {
        when(assistantMessage.getText()).thenReturn(responseText);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    @Test
    void processNaturalLanguageQuery_returnsCorrectParams_whenValidResponse() {
        mockClaudeResponse("""
                  {
                      "longitude": -87.6298,
                      "latitude": 41.8781,
                      "range": 5.0,
                      "locationType": "Chicago, USA",
                      "confidence": "HIGH"
                  }
                  """);

        SpatialQueryParams result = naturalLanguageQueryService.processNaturalLanguageQuery("show me animals near Chicago");

        assertThat(result.latitude()).isEqualTo(41.8781);
        assertThat(result.longitude()).isEqualTo(-87.6298);
        assertThat(result.range()).isEqualTo(5.0);
        assertThat(result.locationType()).isEqualTo("Chicago, USA");
        assertThat(result.confidence()).isEqualTo("HIGH");
    }

    @Test
    void processNaturalLanguageQuery_throwsException_whenClaudeReturnsError() {
        mockClaudeResponse("""
                  {"error": "Could not identify a location from the query"}
                  """);

        assertThrows(NaturalLanguageQueryException.class,
                () -> naturalLanguageQueryService.processNaturalLanguageQuery("asdfjkl"));
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

        assertThrows(NaturalLanguageQueryException.class,
                () -> naturalLanguageQueryService.processNaturalLanguageQuery("show me animals near Chicago"));
    }
}