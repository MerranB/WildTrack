package com.wildtrack.controller;

import com.wildtrack.service.NaturalLanguageQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NaturalLanguageQueryController.class)
class NaturalLanguageQueryControllerTest {

    private static final String ANALYSIS_URL = "/api/v1/analysis/query";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NaturalLanguageQueryService naturalLanguageQueryService;

    @Test
    void query_returnsOk_withValidPrompt() throws Exception {
        when(naturalLanguageQueryService.processNaturalLanguageQuery(any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(ANALYSIS_URL)
                        .param("userPrompt", "show me animals near Chicago"))
                .andExpect(status().isOk());
    }

    @Test
    void query_returnsOk_withPaginationParams() throws Exception {
        when(naturalLanguageQueryService.processNaturalLanguageQuery(any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(ANALYSIS_URL)
                        .param("userPrompt", "show me animals near Chicago")
                        .param("page", "0")
                        .param("size", "5"))
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
