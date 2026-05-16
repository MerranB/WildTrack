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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeoFenceController.class)
class GeoFenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GeoFenceService geoFenceService;

    private GeoFenceDto sampleDto() {
        return new GeoFenceDto(
                "Test Fence",
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(11.0, 20.0),
                        new CoordinateDto(10.0, 20.0)
                ),
                "test@email.com",
                "testuser"
        );
    }

    @Test
    void getAll_returnsOkWithPage() throws Exception {
        Page<GeoFenceDto> page = new PageImpl<>(List.of(sampleDto()));
        when(geoFenceService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/geoFence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returnsOk_whenFound() throws Exception {
        when(geoFenceService.findById(1L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/geoFence/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Fence"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        when(geoFenceService.findById(99L)).thenThrow(new ResourceNotFoundException("GeoFence", 99L));

        mockMvc.perform(get("/api/v1/geoFence/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(geoFenceService.create(any())).thenReturn(sampleDto());

        mockMvc.perform(post("/api/v1/geoFence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Fence"));
    }

    @Test
    void update_returnsOk() throws Exception {
        when(geoFenceService.update(eq(1L), any())).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/geoFence/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Fence"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/geoFence/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNotFound_whenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("GeoFence", 99L))
                .when(geoFenceService).delete(99L);

        mockMvc.perform(delete("/api/v1/geoFence/99"))
                .andExpect(status().isNotFound());
    }
    @Test
    void create_returnsBadRequest_whenEmailIsInvalid() throws Exception {
        GeoFenceDto invalidDto = new GeoFenceDto(
                "Test Fence",
                List.of(new CoordinateDto(10.0, 20.0)),
                "not-an-email",
                "testuser"
    );

        mockMvc.perform(post("/api/v1/geoFence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsbadRequest_whenNameIsBlank() throws Exception {
        GeoFenceDto invalidDto = new GeoFenceDto(
                "",
                List.of(new CoordinateDto(10.0, 20.0)),
                "test@email.com",
                "testuser"
        );

        mockMvc.perform(post("/api/v1/geoFence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}