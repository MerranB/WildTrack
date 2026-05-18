package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.service.MovebankEventService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(MovebankController.class)
class MovebankControllerTest {

    private static final String EVENTS_URL = "/api/v1/events";
    private static final String EVENTS_BY_ID_URL = "/api/v1/events/1";
    private static final String EVENTS_NOT_FOUND_URL = "/api/v1/events/99";
    private static final String EVENTS_UPDATE_URL = "/api/v1/events/updateDatabase/10";
    private static final String EVENTS_UPDATE_URL_30 = "/api/v1/events/updateDatabase/30";
    private static final String EVENTS_BY_BOX_URL = "/api/v1/events/allDataPointsByBox";
    private static final String EVENTS_BY_RANGE_URL = "/api/v1/events/allDataPointsByRange";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovebankEventService movebankService;

    private MovebankEventDto sampleDto() {
        return new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111, -11.1111, "11111111", "111111111");
    }

    @Test
    void getAll_returnsOkWithList() throws Exception {
        Page<MovebankEventDto> page = new PageImpl<>(List.of(sampleDto()));
        when(movebankService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(EVENTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].locationLat").value("11.1111"))
                .andExpect(jsonPath("$.totalElements").value(1));
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
        when(movebankService.updateDatabase(10L))
                .thenThrow(new RuntimeException(String.valueOf(500)));

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateDatabase_full_success() throws Exception {
        when(movebankService.updateDatabase(30L))
                .thenReturn("FULL_SUCCESS");

        mockMvc.perform(post(EVENTS_UPDATE_URL_30))
                .andExpect(status().isOk())
                .andExpect(content().string("FULL_SUCCESS"));
    }

    @Test
    void updateDatabase_partial_success() throws Exception {
        when(movebankService.updateDatabase(10L))
                .thenReturn("PARTIAL_SUCCESS");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isMultiStatus())
                .andExpect(content().string("PARTIAL_SUCCESS"));
    }

    @Test
    void updateDatabase_failure() throws Exception {
        when(movebankService.updateDatabase(10L))
                .thenReturn("FAILURE");

        mockMvc.perform(post(EVENTS_UPDATE_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("FAILURE"));
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