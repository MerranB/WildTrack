package com.wildtrack.service;

import com.wildtrack.client.MovebankClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MovebankIngestionServiceTest {

    @Mock
    MovebankClient Movebankclient;

    @InjectMocks
    MovebankIngestionService MovebankIngestionservice;


    @Test
    void Movebank_getData() throws Exception {
        when(Movebankclient.getData(10L)).thenReturn("timestamp");
        assertThat(MovebankIngestionservice.getData(10L)).contains("timestamp");
    }

    @Test
    void Movebank_getData_fail() throws Exception {
        when(Movebankclient.getData(0L)).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> MovebankIngestionservice.getData(0L));
    }

}
