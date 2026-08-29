package com.wildtrack.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildtrack.config.RateLimitInterceptor;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.wildtrack.support.WithSecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(DemoController.class)
@WithSecurityConfig
class DemoControllerTest {

    private static final String DEMO_URL = "/api/v1/demo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DemoService demoService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }


    @Test
    void geoFenceDemo_returnsOk_withValidRequest() throws Exception {
        GeoFenceDto dto = new GeoFenceDto("Test Fence",
                List.of(new CoordinateDto(1.0, 1.0), new CoordinateDto(1.0, -1.0),
                        new CoordinateDto(-1.0, -1.0), new CoordinateDto(-1.0, 1.0),
                        new CoordinateDto(1.0, 1.0)),
                "test@example.com", "testuser", 0);
        when(demoService.testGeoFenceDemo(any())).thenReturn("yay");

        mockMvc.perform(post(DEMO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"yay\"}"));
    }

    @Test
    void geoFenceDemo_returnsBadRequest_withInvalidEmail() throws Exception {
        GeoFenceDto dto = new GeoFenceDto("Test Fence",
                List.of(new CoordinateDto(1.0, 1.0), new CoordinateDto(1.0, -1.0),
                        new CoordinateDto(-1.0, -1.0), new CoordinateDto(-1.0, 1.0),
                        new CoordinateDto(1.0, 1.0)),
                "not-an-email", "testuser", 0);

        mockMvc.perform(post(DEMO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void geoFenceDemo_returnsBadRequest_withBlankName() throws Exception {
        GeoFenceDto dto = new GeoFenceDto("",
                List.of(new CoordinateDto(1.0, 1.0), new CoordinateDto(1.0, -1.0),
                        new CoordinateDto(-1.0, -1.0), new CoordinateDto(-1.0, 1.0),
                        new CoordinateDto(1.0, 1.0)),
                "test@example.com", "testuser", 0);

        mockMvc.perform(post(DEMO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}