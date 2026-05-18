package com.wildtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.service.GeoFenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeoFenceController.class)
@TestPropertySource(properties = "api.key=test-key")
class GeoFenceControllerTest {

    private static final String TEST_FENCE_NAME = "Test Fence";
    private static final String TEST_USERNAME = "testuser";
    private static final String GEOFENCE_URL = "/api/v1/geoFence";
    private static final String GEOFENCE_URL_CREATE = "/api/v1/geoFence/create";
    private static final String GEOFENCE_BY_ID_URL = "/api/v1/geoFence/1";
    private static final String GEOFENCE_NOT_FOUND_URL = "/api/v1/geoFence/99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GeoFenceService geoFenceService;

    private MockHttpServletRequestBuilder withApiKey(MockHttpServletRequestBuilder request) {
        return request.header("X-API-Key", "test-key");
    }

    private GeoFenceDto sampleDto() {
        return new GeoFenceDto(
                TEST_FENCE_NAME,
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(11.0, 20.0),
                        new CoordinateDto(10.0, 20.0)
                ),
                "test@email.com",
                TEST_USERNAME, 0
        );
    }

    @Test
    void getAll_returnsOkWithPage() throws Exception {
        Page<GeoFenceDto> page = new PageImpl<>(List.of(sampleDto()));
        when(geoFenceService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(withApiKey(get(GEOFENCE_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returnsOk_whenFound() throws Exception {
        when(geoFenceService.findById(1L)).thenReturn(sampleDto());

        mockMvc.perform(withApiKey(get(GEOFENCE_BY_ID_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEST_FENCE_NAME));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        when(geoFenceService.findById(99L)).thenThrow(new ResourceNotFoundException("GeoFence", 99L));

        mockMvc.perform(withApiKey(get(GEOFENCE_NOT_FOUND_URL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(geoFenceService.create(any())).thenReturn(sampleDto());

        mockMvc.perform(withApiKey(post(GEOFENCE_URL_CREATE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(TEST_FENCE_NAME));
    }

    @Test
    void update_returnsOk() throws Exception {
        when(geoFenceService.update(eq(1L), any())).thenReturn(sampleDto());

        mockMvc.perform(withApiKey(put(GEOFENCE_BY_ID_URL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEST_FENCE_NAME));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(withApiKey(delete(GEOFENCE_BY_ID_URL)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNotFound_whenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("GeoFence", 99L))
                .when(geoFenceService).delete(99L);

        mockMvc.perform(withApiKey(delete(GEOFENCE_NOT_FOUND_URL)))
                .andExpect(status().isNotFound());
    }
}