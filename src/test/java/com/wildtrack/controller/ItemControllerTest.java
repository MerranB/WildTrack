package com.wildtrack.controller;

import com.wildtrack.dto.ItemDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ItemService itemService;

    @Test
    void getAll_returnsOkWithList() throws Exception {
        Page<ItemDto> page = new PageImpl<>(List.of(ItemDto.of("Wolf", "Gray wolf")));
        when(itemService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Wolf"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returnsOk_whenFound() throws Exception {
        when(itemService.findById(1L)).thenReturn(ItemDto.of("Wolf", "Gray wolf"));

        mockMvc.perform(get("/api/v1/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wolf"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        when(itemService.findById(99L)).thenThrow(new ResourceNotFoundException("Item", 99L));

        mockMvc.perform(get("/api/v1/items/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated() throws Exception {
        ItemDto inputDto = ItemDto.of("Wolf", "Gray wolf");
        ItemDto savedDto = new ItemDto(1L, "Wolf", "Gray wolf", null, null);
        when(itemService.create(any(ItemDto.class))).thenReturn(savedDto);

        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Wolf"));
    }

    @Test
    void create_returnsBadRequest_whenNameIsBlank() throws Exception {
        ItemDto invalidDto = new ItemDto(null, "", "desc", null, null);

        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));
    }

    @Test
    void update_returnsOk() throws Exception {
        ItemDto updatedDto = new ItemDto(1L, "Updated Wolf", "Updated desc", null, null);
        when(itemService.update(eq(1L), any(ItemDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ItemDto.of("Updated Wolf", "Updated desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Wolf"));
    }

    @Test
    void update_returnsNotFound_whenMissing() throws Exception {
        when(itemService.update(eq(99L), any(ItemDto.class)))
                .thenThrow(new ResourceNotFoundException("Item", 99L));

        mockMvc.perform(put("/api/v1/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ItemDto.of("Name", "Desc"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(itemService).delete(1L);

        mockMvc.perform(delete("/api/v1/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNotFound_whenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Item", 99L)).when(itemService).delete(99L);

        mockMvc.perform(delete("/api/v1/items/99"))
                .andExpect(status().isNotFound());
    }
}
