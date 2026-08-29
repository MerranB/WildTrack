package com.wildtrack.service;

import com.opencsv.CSVReader;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.MovebankHeaderNormalizer;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.repository.MovebankEventBatchWriter;
import com.wildtrack.service.MovebankStudyIngestor.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovebankStudyIngestorTest {

    private static final Long STUDY_ID = 19186107L;

    @Mock
    private MovebankClient movebankClient;

    @Spy
    private MovebankHeaderNormalizer movebankHeaderNormalizer = new MovebankHeaderNormalizer();

    @Mock
    private MovebankEventBatchWriter movebankEventBatchWriter;

    @InjectMocks
    private MovebankStudyIngestor movebankStudyIngestor;

    @Captor
    private ArgumentCaptor<List<MovebankEventDto>> batchCaptor;

    private void stubAllInserted() {
        when(movebankEventBatchWriter.insertBatch(any()))
                .thenAnswer(invocation -> invocation.<List<MovebankEventDto>>getArgument(0).size());
    }

    @Test
    void ingestStudy_fail() {
        when(movebankClient.getData(0L)).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> movebankStudyIngestor.ingestStudy(0L));
    }

    @Test
    void ingestStudy_nullData_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(null);

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
        verify(movebankEventBatchWriter, never()).insertBatch(any());
    }
    @Test
    void ingestStudy_emptyFile_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(tempCsv(""));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
        verify(movebankEventBatchWriter, never()).insertBatch(any());
    }

    @Test
    void ingestStudy_parsesCorrectData() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("CorrectCSV.csv"));
        stubAllInserted();

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventBatchWriter).insertBatch(batchCaptor.capture());
        MovebankEventDto parsed = batchCaptor.getValue().getFirst();

        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
        assertThat(parsed.getTimestamp()).isEqualTo(LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000));
        assertThat(parsed.getLocationLat()).isEqualTo(11.1111d);
        assertThat(parsed.getLocationLong()).isEqualTo(-11.1111d);
        assertThat(parsed.getIndividualId()).isEqualTo("11111111");
        assertThat(parsed.getTagId()).isEqualTo("111111111");
    }
    @ParameterizedTest(name = "{1}")
    @CsvSource({
            "HeadersOnly.csv,             a CSV with headers but no data rows",
            "MissingRows.csv,             rows missing required fields",
            "MovebankUnmappedHeaders.csv, headers the normalizer does not recognise"
    })
    void ingestStudy_returnsNoValidDataAndWritesNothing(String fixture, String scenario) {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile(fixture));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).as(scenario).isEqualTo(Result.NO_VALID_DATA);
        verify(movebankEventBatchWriter, never()).insertBatch(any());
    }

    @Test
    void ingestStudy_handsValidRecordsToTheBatchWriter() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("CorrectCSV.csv"));
        stubAllInserted();

        movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventBatchWriter).insertBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(1);
    }

    @Test
    void ingestStudy_oneChunkFails_remainingChunksStillRun() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MultiRecords.csv"));
        when(movebankEventBatchWriter.insertBatch(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenAnswer(invocation -> invocation.<List<MovebankEventDto>>getArgument(0).size());

        movebankStudyIngestor.ingestStudy(STUDY_ID);
        verify(movebankEventBatchWriter, times(24)).insertBatch(any());
    }

    @Test
    void ingestStudy_oneChunkFails_returnsPartialSuccess() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MultiRecords.csv"));
        when(movebankEventBatchWriter.insertBatch(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenAnswer(invocation -> invocation.<List<MovebankEventDto>>getArgument(0).size());

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.PARTIAL_SUCCESS);
    }

    @Test
    void ingestStudy_over20PercentFail_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MultiRecords.csv"));
        when(movebankEventBatchWriter.insertBatch(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenAnswer(invocation -> invocation.<List<MovebankEventDto>>getArgument(0).size());

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
    }

    @Test
    void ingestStudy_allChunksFail_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MultiRecords.csv"));
        when(movebankEventBatchWriter.insertBatch(any()))
                .thenThrow(new RuntimeException("Simulated failure"));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
    }

    @Test
    void ingestStudy_duplicateRecords_areSkippedNotFailed() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MultiRecords.csv"));
        when(movebankEventBatchWriter.insertBatch(any())).thenReturn(0);

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
        verify(movebankEventBatchWriter, times(24)).insertBatch(any());
    }

    @Test
    void ingestStudy_movebankDashedHeaders_bindsEveryField() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MovebankRawHeaders.csv"));
        stubAllInserted();

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventBatchWriter).insertBatch(batchCaptor.capture());
        List<MovebankEventDto> written = batchCaptor.getValue();
        MovebankEventDto first = written.getFirst();

        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
        assertThat(written).hasSize(3);
        assertThat(first.getTimestamp()).isEqualTo(LocalDateTime.of(2015, 4, 12, 14, 22, 33));
        assertThat(first.getLocationLat()).isEqualTo(33.5678d);
        assertThat(first.getLocationLong()).isEqualTo(-117.1234d);
        assertThat(first.getIndividualId()).isEqualTo("HAWK-IND-01");
        assertThat(first.getTagId()).isEqualTo("HAWK-TAG-01");
    }

    @Test
    void ingestStudy_dashedHeadersWithBadRows_writesOnlyTheValidOnes() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsTempFile("MovebankRawHeadersMixed.csv"));
        stubAllInserted();

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventBatchWriter).insertBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(2);
        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
    }

    @Test
    void ingestStudy_stripsByteOrderMarkBeforeReadingHeaders() {
        String withBom = "﻿timestamp,location-long,location-lat,individual-local-identifier,tag-local-identifier\n"
                + "2015-07-04 12:00:00.000,-120.5000,36.5000,HAWK-IND-30,HAWK-TAG-30";
        when(movebankClient.getData(STUDY_ID)).thenReturn(tempCsv(withBom));
        stubAllInserted();

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventBatchWriter).insertBatch(batchCaptor.capture());
        MovebankEventDto written = batchCaptor.getValue().getFirst();

        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
        assertThat(written.getTimestamp()).isEqualTo(LocalDateTime.of(2015, 7, 4, 12, 0, 0));
        assertThat(written.getTagId()).isEqualTo("HAWK-TAG-30");
    }

    Path loadCsvAsTempFile(String filename) {
        return tempCsv(loadCsvAsString(filename));
    }

    Path tempCsv(String content) {
        try {
            Path temp = Files.createTempFile("ingestor-test-", ".csv");
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            return temp;
        } catch (IOException e) {
            Assertions.fail("Failed to write temp CSV", e);
            return null;
        }
    }

    String loadCsvAsString(String filename) {
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
            Assertions.fail("Failed to load CSV file: " + filename, e);
        }
        return csvValues;
    }
}
