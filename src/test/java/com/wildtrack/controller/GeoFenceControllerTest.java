package com.wildtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.config.RateLimitInterceptor;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.service.GeoFenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import com.wildtrack.support.WithSecurityConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@WebMvcTest(GeoFenceController.class)
@WithSecurityConfig
class GeoFenceControllerTest {

    private static final String TEST_FENCE_NAME = "Test Fence";
    private static final String TEST_USERNAME = "testuser";
    private static final String GEOFENCE_URL = "/api/v1/geoFence";
    private static final String GEOFENCE_BY_ID_URL = "/api/v1/geoFence/1";
    private static final String GEOFENCE_NOT_FOUND_URL = "/api/v1/geoFence/99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GeoFenceService geoFenceService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
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

        mockMvc.perform(get(GEOFENCE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returnsOk_whenFound() throws Exception {
        when(geoFenceService.findById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get(GEOFENCE_BY_ID_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEST_FENCE_NAME));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        when(geoFenceService.findById(99L)).thenThrow(new ResourceNotFoundException("GeoFence", 99L));

        mockMvc.perform(get(GEOFENCE_NOT_FOUND_URL))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(geoFenceService.create(any())).thenReturn(sampleDto());

        mockMvc.perform(post(GEOFENCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(TEST_FENCE_NAME));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returnsOk() throws Exception {
        when(geoFenceService.update(eq(1L), any())).thenReturn(sampleDto());

        mockMvc.perform(put(GEOFENCE_BY_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEST_FENCE_NAME));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete(GEOFENCE_BY_ID_URL))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returnsNotFound_whenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("GeoFence", 99L))
                .when(geoFenceService).delete(99L);

        mockMvc.perform(delete(GEOFENCE_NOT_FOUND_URL))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsUnauthorized_whenNoCredentialsSupplied() throws Exception {
        mockMvc.perform(delete(GEOFENCE_BY_ID_URL))
                .andExpect(status().isUnauthorized());

        verify(geoFenceService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_returnsForbidden_whenAuthenticatedUserIsNotAdmin() throws Exception {
        mockMvc.perform(put(GEOFENCE_BY_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isForbidden());

        verify(geoFenceService, never()).update(any(), any());
    }
}
