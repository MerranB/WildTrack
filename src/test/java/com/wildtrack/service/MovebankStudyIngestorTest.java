package com.wildtrack.service;

import com.opencsv.CSVReader;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.MovebankHeaderNormalizer;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import com.wildtrack.service.MovebankStudyIngestor.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    // Real normalizer — a no-op on the canonical (underscore) headers the test CSVs use.
    @Spy
    private MovebankHeaderNormalizer movebankHeaderNormalizer = new MovebankHeaderNormalizer();

    @Mock
    private MovebankEventRepository movebankEventRepository;

    @Mock
    private MovebankEventMapper movebankEventMapper;

    @InjectMocks
    private MovebankStudyIngestor movebankStudyIngestor;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUpPoint() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
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
        verify(movebankEventMapper, never()).toEntity(any());
    }

    @Test
    void ingestStudy_emptyData_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn("");

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
        verify(movebankEventMapper, never()).toEntity(any());
    }

    @Test
    void ingestStudy_parsesCorrectData() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("CorrectCSV.csv"));
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                location, "11111111", "111111111"
        ));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        ArgumentCaptor<MovebankEventDto> captor = ArgumentCaptor.forClass(MovebankEventDto.class);

        Result msg = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventMapper).toEntity(captor.capture());
        MovebankEventDto parsed = captor.getValue();

        assertThat(msg).isEqualTo(Result.FULL_SUCCESS);
        assertThat(parsed.getTimestamp()).isEqualTo(LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000));
        assertThat(parsed.getLocationLat()).isEqualTo(11.1111d);
        assertThat(parsed.getLocationLong()).isEqualTo(-11.1111d);
        assertThat(parsed.getIndividualId()).isEqualTo("11111111");
        assertThat(parsed.getTagId()).isEqualTo("111111111");
    }

    @Test
    void ingestStudy_withHeaderOnlyCSV_returnsNoValidData() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("HeadersOnly.csv"));

        Result msg = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventMapper, never()).toEntity(any());
        assertThat(msg).isEqualTo(Result.NO_VALID_DATA);
    }

    @Test
    void ingestStudy_savesNewRecord() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("CorrectCSV.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                location, "11111111", "111111111"
        ));

        movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventRepository).save(any(MovebankEvent.class));
    }

    @Test
    void ingestStudy_oneRecordFails_othersStillSave() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(new MovebankEvent(
                        LocalDateTime.of(2012, 1, 21, 2, 1, 21, 212000000),
                        location, "12121212", "121212121"
                ));

        movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventRepository, times(23)).save(any(MovebankEvent.class));
    }

    @Test
    void ingestStudy_oneRecordFails_returnsPartialSuccess() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(new MovebankEvent(
                        LocalDateTime.of(2012, 1, 21, 2, 1, 21, 212000000),
                        location, "12121212", "121212121"
                ));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.PARTIAL_SUCCESS);
    }

    @Test
    void ingestStudy_over20PercentFail_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(new MovebankEvent(
                        LocalDateTime.of(2012, 1, 21, 2, 1, 21, 212000000),
                        location, "12121212", "121212121"
                ));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
    }

    @Test
    void ingestStudy_allRecordsFail_returnsFailure() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.FAILURE);
    }

    @Test
    void ingestStudy_duplicateRecords_areSkipped() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                location, "11111111", "111111111"
        ));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(true);

        movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventRepository, never()).save(any(MovebankEvent.class));
    }

    @Test
    void ingestStudy_recordsMissingRequiredFields_noneReachMapper() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MissingRows.csv"));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        // Rows arrived but every one failed validation — the study yielded nothing usable,
        // which must not be reported as success.
        assertThat(result).isEqualTo(Result.NO_VALID_DATA);
        verify(movebankEventMapper, never()).toEntity(any());
    }

    // --- Raw Movebank headers -------------------------------------------------
    // The four original fixtures all use canonical underscore headers, so the
    // normalizer is a no-op in them. These exercise the header rewriting that
    // ingestion of a second study actually depends on.

    @Test
    void ingestStudy_movebankDashedHeaders_bindsEveryField() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MovebankRawHeaders.csv"));
        Point location = geometryFactory.createPoint(new Coordinate(-117.1234d, 33.5678d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2015, 4, 12, 14, 22, 33),
                location, "HAWK-IND-01", "HAWK-TAG-01"
        ));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        ArgumentCaptor<MovebankEventDto> captor = ArgumentCaptor.forClass(MovebankEventDto.class);

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventMapper, times(3)).toEntity(captor.capture());
        MovebankEventDto first = captor.getAllValues().getFirst();

        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
        assertThat(first.getTimestamp()).isEqualTo(LocalDateTime.of(2015, 4, 12, 14, 22, 33));
        assertThat(first.getLocationLat()).isEqualTo(33.5678d);
        assertThat(first.getLocationLong()).isEqualTo(-117.1234d);
        assertThat(first.getIndividualId()).isEqualTo("HAWK-IND-01");
        assertThat(first.getTagId()).isEqualTo("HAWK-TAG-01");
    }

    @Test
    void ingestStudy_movebankDashedHeaders_savesEveryValidRecord() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MovebankRawHeaders.csv"));
        Point location = geometryFactory.createPoint(new Coordinate(-117.1234d, 33.5678d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2015, 4, 12, 14, 22, 33),
                location, "HAWK-IND-01", "HAWK-TAG-01"
        ));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);

        movebankStudyIngestor.ingestStudy(STUDY_ID);

        verify(movebankEventRepository, times(3)).save(any(MovebankEvent.class));
    }

    @Test
    void ingestStudy_dashedHeadersWithBadRows_savesOnlyTheValidOnes() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MovebankRawHeadersMixed.csv"));
        Point location = geometryFactory.createPoint(new Coordinate(-118.1111d, 34.1111d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2015, 5, 1, 10, 0, 0),
                location, "HAWK-IND-10", "HAWK-TAG-10"
        ));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        // Four rows in, two missing a required field — those are dropped, not failed,
        // so the run is still a full success.
        verify(movebankEventMapper, times(2)).toEntity(any());
        verify(movebankEventRepository, times(2)).save(any(MovebankEvent.class));
        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
    }

    // Regression guard for the alias map: empty it or break an entry and the bound
    // fields all come back null, every row is filtered, and this flips to NO_VALID_DATA.
    @Test
    void ingestStudy_headersTheNormalizerDoesNotRecognise_returnsNoValidData() {
        when(movebankClient.getData(STUDY_ID)).thenReturn(loadCsvAsString("MovebankUnmappedHeaders.csv"));

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        assertThat(result).isEqualTo(Result.NO_VALID_DATA);
        verify(movebankEventMapper, never()).toEntity(any());
    }

    @Test
    void ingestStudy_stripsByteOrderMarkBeforeReadingHeaders() {
        String withBom = "﻿timestamp,location-long,location-lat,individual-local-identifier,tag-local-identifier\n"
                + "2015-07-04 12:00:00.000,-120.5000,36.5000,HAWK-IND-30,HAWK-TAG-30";
        when(movebankClient.getData(STUDY_ID)).thenReturn(withBom);
        Point location = geometryFactory.createPoint(new Coordinate(-120.5d, 36.5d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2015, 7, 4, 12, 0, 0),
                location, "HAWK-IND-30", "HAWK-TAG-30"
        ));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        ArgumentCaptor<MovebankEventDto> captor = ArgumentCaptor.forClass(MovebankEventDto.class);

        Result result = movebankStudyIngestor.ingestStudy(STUDY_ID);

        // A surviving BOM would corrupt the first header token and null the timestamp,
        // which would drop the only row and return NO_VALID_DATA instead.
        verify(movebankEventMapper).toEntity(captor.capture());
        assertThat(result).isEqualTo(Result.FULL_SUCCESS);
        assertThat(captor.getValue().getTimestamp()).isEqualTo(LocalDateTime.of(2015, 7, 4, 12, 0, 0));
        assertThat(captor.getValue().getTagId()).isEqualTo("HAWK-TAG-30");
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
