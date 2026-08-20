package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.config.RateLimitInterceptor;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.service.MovebankEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovebankController.class)
class MovebankControllerTest {

    private static final String EVENTS_URL = "/api/v1/events/all";
    private static final String EVENTS_BY_ID_URL = "/api/v1/events/1";
    private static final String EVENTS_NOT_FOUND_URL = "/api/v1/events/99";
    private static final String EVENTS_UPDATE_URL = "/api/v1/events/updateDatabase";
    private static final String EVENTS_BY_BOX_URL = "/api/v1/events/allDataPointsByBox";
    private static final String EVENTS_BY_RANGE_URL = "/api/v1/events/allDataPointsByRange";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovebankEventService movebankService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private MovebankEventDto sampleDto() {
        return new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111, -11.1111, "11111111", "111111111");
    }

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void getAll_returnsOkWithPage() throws Exception {
        when(movebankService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleDto())));

        mockMvc.perform(get(EVENTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].locationLat").value(11.1111))
                .andExpect(jsonPath("$.content.length()").value(1));
    }


    @Test
    void getById_returnsOk_whenFound() throws Exception {
        when(movebankService.findById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get(EVENTS_BY_ID_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationLat").value("11.1111"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        when(movebankService.findById(99L)).thenThrow(new ResourceNotFoundException("MovebankEvent", 99L));

        mockMvc.perform(get(EVENTS_NOT_FOUND_URL))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDatabase_returns_non200() throws Exception {
        when(movebankService.updateDatabase())
                .thenThrow(new RuntimeException(String.valueOf(500)));

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError());
    }

    // Stubs use the real summary format the service produces — "<RESULT> for <studyId>",
    // one line per study — so the controller's substring classification is exercised
    // against the strings it actually receives in production.

    @Test
    void updateDatabase_allStudiesFullSuccess_returns200() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("FULL_SUCCESS for 19186107\nFULL_SUCCESS for 1073231887\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("FULL_SUCCESS for 19186107")));
    }

    @Test
    void updateDatabase_partialSuccess_returns207() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("PARTIAL_SUCCESS for 19186107\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.message").value(containsString("PARTIAL_SUCCESS")));
    }

    @Test
    void updateDatabase_oneStudySucceedsOneHasNoValidData_returns207() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("FULL_SUCCESS for 19186107\nNO_VALID_DATA for 1073231887\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.message").value(containsString("NO_VALID_DATA for 1073231887")));
    }

    @Test
    void updateDatabase_noStudyHasValidData_returns422() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("NO_VALID_DATA for 19186107\nNO_VALID_DATA for 1073231887\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("NO_VALID_DATA")));
    }

    @Test
    void updateDatabase_failure_returns500() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("FAILURE for 19186107\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("FAILURE")));
    }

    @Test
    void updateDatabase_noValidDataAlongsideFailure_returns500() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("NO_VALID_DATA for 19186107\nFAILURE for 1073231887\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateDatabase_studyThrew_returns500() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("Study 19186107 FAILED - Connection refused\n");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("FAILED")));
    }

    @Test
    void updateDatabase_noStudyIdsConfigured_returns500() throws Exception {
        when(movebankService.updateDatabase())
                .thenReturn("FAILED - No study IDs configured");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void allDataPointsByBox_returnsOk_withValidParams() throws Exception {
        Page<MovebankEventDto> page = new PageImpl<>(List.of(sampleDto()));
        when(movebankService.allDataPointsByBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(EVENTS_BY_BOX_URL)
                        .param("minLon", "-10.0").param("minLat", "-10.0")
                        .param("maxLon", "10.0").param("maxLat", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void allDataPointsByBox_returnsBadRequest_whenLonOutOfRange() throws Exception {
        mockMvc.perform(get(EVENTS_BY_BOX_URL)
                        .param("minLon", "181.0").param("minLat", "0.0")
                        .param("maxLon", "10.0").param("maxLat", "10.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allDataPointsByBox_returnsBadRequest_whenLatOutOfRange() throws Exception {
        mockMvc.perform(get(EVENTS_BY_BOX_URL)
                        .param("minLon", "0.0").param("minLat", "91.0")
                        .param("maxLon", "10.0").param("maxLat", "10.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allDataPointsByRange_returnsOk_withValidParams() throws Exception {
        Page<MovebankEventDto> page = new PageImpl<>(List.of(sampleDto()));
        when(movebankService.allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(EVENTS_BY_RANGE_URL)
                        .param("lon", "0.0").param("lat", "0.0").param("range", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void allDataPointsByRange_returnsBadRequest_whenLonOutOfRange() throws Exception {
        mockMvc.perform(get(EVENTS_BY_RANGE_URL)
                        .param("lon", "181.0").param("lat", "0.0").param("range", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allDataPointsByRange_returnsBadRequest_whenLatOutOfRange() throws Exception {
        mockMvc.perform(get(EVENTS_BY_RANGE_URL)
                        .param("lon", "0.0").param("lat", "91.0").param("range", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allDataPointsByRange_returnsBadRequest_whenRangeOutOfRange() throws Exception {
        mockMvc.perform(get(EVENTS_BY_RANGE_URL)
                        .param("lon", "0.0").param("lat", "0.0").param("range", "11.0"))
                .andExpect(status().isBadRequest());
    }
}