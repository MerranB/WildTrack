package com.wildtrack.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
 class MovebankClientTest {

    @InjectMocks
    MovebankClient Movebankclient;

    @Mock
    HttpResponse<Object> mockResponse;

    @Mock
    HttpClient client;

    @Mock
    CookieManager cookieManager;

    @BeforeEach
    void setup(){
        ReflectionTestUtils.setField(Movebankclient, "MBUS", "X");
        ReflectionTestUtils.setField(Movebankclient, "MBPW", "Y");
    }

    @Test
    void Movebank_getData() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("timestamp");
        when(client.send(any(), any())).thenReturn(mockResponse);

        assertThat(Movebankclient.getData(10L)).contains("timestamp");
    }

    @Test
    void Movebank_getData_fail() throws Exception {
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn(null);

        when(client.send(any(), any())).thenReturn(mockResponse);
        assertThrows(RuntimeException.class, () -> Movebankclient.getData(10L));
    }

    @Test
    void Movebank_getData_invalid_ID() throws Exception {
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn(null);

        when(client.send(any(), any())).thenReturn(mockResponse);
        assertThrows(RuntimeException.class, () -> Movebankclient.getData(15L));
    }
}
