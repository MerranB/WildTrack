package com.wildtrack.service;

import com.opencsv.CSVReader;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.dto.MovebankEventDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MovebankIngestionServiceTest {

    private static final Logger log = LoggerFactory.getLogger(MovebankIngestionServiceTest.class);
    @Mock
    MovebankClient Movebankclient;

    @InjectMocks
    MovebankIngestionService MovebankIngestionservice;

    @Test
    void Movebank_getData_fail() {
        when(Movebankclient.getData(0L)).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> MovebankIngestionservice.getData(0L));
    }
    @Test
    void Movebank_parseValidCSV() throws IOException {


        when(Movebankclient.getData(10L)).thenReturn(loadCsvAsString("CorrectCSV.csv"));

        List<MovebankEventDto> result = MovebankIngestionservice.getData(10L);

        assertThat(result.getFirst().getTimestamp()).isEqualTo(LocalDateTime.of(2011,1,11,1,1,11, 111000000));
        assertThat(result.getFirst().getLocation_lat()).isEqualTo(11.1111d);
        assertThat(result.getFirst().getLocation_long()).isEqualTo(-11.1111d);
        assertThat(result.getFirst().getIndividual_id()).isEqualTo("11111111");
        assertThat(result.getFirst().getTag_id()).isEqualTo("111111111");

        }
    @Test
    void Movebank_parseHeaderOnlyCSV() throws IOException {

        when(Movebankclient.getData(10L)).thenReturn(loadCsvAsString("HeadersOnly.csv"));

        List<MovebankEventDto> result = MovebankIngestionservice.getData(10L);

        assertThat(result).isEmpty();


    }
    @Test
    void Movebank_parseCSVWithMissingRows() throws IOException {

        when(Movebankclient.getData(10L)).thenReturn(loadCsvAsString("MissingRows.csv"));

        List<MovebankEventDto> result = MovebankIngestionservice.getData(10L);

        assertThat(result.getFirst().getTimestamp()).isEqualTo(LocalDateTime.of(2022,2,22,2,2,22, 222000000));
        assertThat(result.getFirst().getLocation_lat()).isEqualTo(null);
        assertThat(result.getFirst().getLocation_long()).isEqualTo(-22.2222d);
        assertThat(result.getFirst().getIndividual_id()).isEqualTo("22222222");
        assertThat(result.getFirst().getTag_id()).isEqualTo("222222222");
    }

    String loadCsvAsString(String filename){
        String csvValues = "";

        try (InputStream is = getClass().getResourceAsStream("/" + filename)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found in resources!");
            }

            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 CSVReader reader = new CSVReader(isr)) {

                List<String[]> records = reader.readAll();
                csvValues = records.stream()
                        .map(row -> String.join(",", row))
                        .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("e: ", e);
        }
        return csvValues;
    }
}
