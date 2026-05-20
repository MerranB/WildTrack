package com.wildtrack.controller;

import com.wildtrack.analysis.SpatialQueryParams;
import com.wildtrack.service.NaturalLanguageQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NaturalLanguageQueryController.class)
class NaturalLanguageQueryControllerTest {

    private static final String ANALYSIS_URL = "/api/v1/analysis";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NaturalLanguageQueryService naturalLanguageQueryService;


    @Test
    void query_returnsOk_withValidPrompt() throws Exception {
        SpatialQueryParams params = new SpatialQueryParams(-87.6298, 41.8781, 5.0, "Chicago, USA", "HIGH");
        when(naturalLanguageQueryService.processNaturalLanguageQuery(any())).thenReturn(params);

        mockMvc.perform(get(ANALYSIS_URL)
                        .param("userPrompt", "show me animals near Chicago"))
                .andExpect(status().isOk());
    }

    @Test
    void query_returnsBadRequest_whenPromptIsMissing() throws Exception {
        mockMvc.perform(get(ANALYSIS_URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    void query_returnsBadRequest_whenPromptIsEmpty() throws Exception {
        mockMvc.perform(get(ANALYSIS_URL)
                        .param("userPrompt", ""))
                .andExpect(status().isBadRequest());
    }
}