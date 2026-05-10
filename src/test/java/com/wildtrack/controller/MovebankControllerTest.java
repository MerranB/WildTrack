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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovebankController.class)
class MovebankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovebankEventService movebankService;

    @Test
    void getAll_returnsOkWithList() throws Exception {
        Page<MovebankEventDto> page = new PageImpl<>(List.of(
                new MovebankEventDto(LocalDateTime.of(2011,1,11,1,1,11, 111000000),
                        11.1111,-11.1111,"11111111","111111111")));
        when(movebankService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].locationLat").value("11.1111"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returnsOk_whenFound() throws Exception {
        when(movebankService.findById(1L)).thenReturn(new MovebankEventDto(LocalDateTime.of(2011,1,11,1,1,11, 111000000),
                11.1111,-11.1111,"11111111","111111111"));

        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationLat").value("11.1111"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        when(movebankService.findById(99L)).thenThrow(new ResourceNotFoundException("MovebankEvent", 99L));

        mockMvc.perform(get("/api/v1/events/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDatabase_returns_non200() throws Exception {
        when(movebankService.updateDatabase(10L))
                .thenThrow(new RuntimeException(String.valueOf(500)));
        mockMvc.perform(post("/api/v1/events/updateDatabase/10"))
                .andExpect(status().isInternalServerError());
    }

        @Test
    void updateDatabase_full_success() throws Exception {

        when(movebankService.updateDatabase(30L))
                .thenReturn("FULL_SUCCESS");
        mockMvc.perform(post("/api/v1/events/updateDatabase/30"))
                .andExpect(status().isOk())
                .andExpect(content().string("FULL_SUCCESS"));
    }

    @Test
    void updateDatabase_partial_success() throws Exception {
        when(movebankService.updateDatabase(10L))
                .thenReturn("PARTIAL_SUCCESS");
        mockMvc.perform(post("/api/v1/events/updateDatabase/10"))
                .andExpect(status().isMultiStatus())
                .andExpect(content().string("PARTIAL_SUCCESS"));
    }

    @Test
    void updateDatabase_failure() throws Exception {
        when(movebankService.updateDatabase(10L))
                .thenReturn("FAILURE");
        mockMvc.perform(post("/api/v1/events/updateDatabase/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("FAILURE"));
    }
}
