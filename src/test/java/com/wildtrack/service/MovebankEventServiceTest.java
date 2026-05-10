package com.wildtrack.service;

import com.opencsv.CSVReader;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import org.locationtech.jts.geom.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovebankEventServiceTest {

    private static final Logger log = LoggerFactory.getLogger(MovebankEventServiceTest.class);
    @Mock
    MovebankClient movebankClient;

    @Mock
    MovebankEventRepository movebankEventRepository;

    @Mock
    MovebankEventMapper movebankEventMapper;

    @InjectMocks
    MovebankEventService movebankEventService;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUpPoint() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Test
    void updateDatabase_fail() {
        when(movebankClient.getData(0L)).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> movebankEventService.updateDatabase(0L));
    }
    @Test
    void updateDatabase_parsesCorrectData(){
        when(movebankClient.getData(10L)).thenReturn(loadCsvAsString("CorrectCSV.csv"));
        Point point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(-22.2222, 22.2222));
        MovebankEvent entity = new MovebankEvent(LocalDateTime.now(), point, "22222222", "222222222");
        when(movebankEventMapper.toEntity(any())).thenReturn(entity);
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        ArgumentCaptor<MovebankEventDto> captor = ArgumentCaptor.forClass(MovebankEventDto.class);

        String msg = movebankEventService.updateDatabase(10L);

        verify(movebankEventMapper).toEntity(captor.capture());
        MovebankEventDto parsed = captor.getValue();

        assertThat(msg).contains("FULL_SUCCESS");
        assertThat(parsed.getTimestamp()).isEqualTo(LocalDateTime.of(2011,1,11,1,1,11, 111000000));
        assertThat(parsed.getLocationLat()).isEqualTo(11.1111d);
        assertThat(parsed.getLocationLong()).isEqualTo(-11.1111d);
        assertThat(parsed.getIndividualId()).isEqualTo("11111111");
        assertThat(parsed.getTagId()).isEqualTo("111111111");

    }
    @Test
    void updateDatabase_withHeaderOnlyCSV(){

        when(movebankClient.getData(20L)).thenReturn(loadCsvAsString("HeadersOnly.csv"));

        String msg = movebankEventService.updateDatabase(20L);

        verify(movebankEventMapper, never()).toEntity(any());
        assertThat(msg).contains("FULL_SUCCESS");
    }
    @Test
    void updateDatabase_withMissingField(){

        when(movebankClient.getData(30L)).thenReturn(loadCsvAsString("MissingRows.csv"));
        Point point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(-22.2222, 22.2222));
        MovebankEvent entity = new MovebankEvent(LocalDateTime.now(), point, "22222222", "222222222");
        when(movebankEventMapper.toEntity(any())).thenReturn(entity);
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        ArgumentCaptor<MovebankEventDto> captor = ArgumentCaptor.forClass(MovebankEventDto.class);
        String msg = movebankEventService.updateDatabase(30L);

        verify(movebankEventMapper, times(5)).toEntity(captor.capture());
        List<MovebankEventDto>  allParsed = captor.getAllValues();

        MovebankEventDto nullLatRecord = allParsed.stream()
                .filter(dto -> dto.getLocationLat() == null)
                .findFirst()
                .orElseThrow();

        assertThat(msg).contains("FULL_SUCCESS");
        assertThat(nullLatRecord.getTimestamp()).isEqualTo(LocalDateTime.of(2022,2,22,2,2,22, 222000000));
        assertThat(nullLatRecord.getLocationLat()).isNull();
        assertThat(nullLatRecord.getLocationLong()).isEqualTo(-22.2222d);
        assertThat(nullLatRecord.getIndividualId()).isEqualTo("22222222");
        assertThat(nullLatRecord.getTagId()).isEqualTo("222222222");
    }

    @Test
    void updateDatabase_savesNewRecord(){
        when(movebankClient.getData(10L)).thenReturn(loadCsvAsString("CorrectCSV.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d,-12.1212d));
        when(movebankEventMapper.toEntity(any())).thenReturn(new MovebankEvent(
                LocalDateTime.of(2011,1,11,1,1,11,111000000),
                location,  "11111111", "111111111"
        ));

        movebankEventService.updateDatabase(10L);

        verify(movebankEventRepository).save(any(MovebankEvent.class));
    }

    @Test
    void findAll_returnsPageOfDtos() {
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d,-12.1212d));
        MovebankEvent entity = new MovebankEvent(
                LocalDateTime.of(2011,1,11,1,1,11,111000000),
                location, "11111111", "111111111"
        );
        Page<MovebankEvent> page = new PageImpl<>(List.of(entity));
        when(movebankEventRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011,1,11,1,1,11,111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));

        Page<MovebankEventDto> result = movebankEventService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTimestamp())
                .isEqualTo(LocalDateTime.of(2011,1,11,1,1,11,111000000));
    }

    @Test
    void findById_returnsCorrectDto() {
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d,-12.1212d));
        MovebankEvent entity = new MovebankEvent(
                LocalDateTime.of(2011,1,11,1,1,11,111000000),
                location, "11111111", "111111111"
        );
        when(movebankEventRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011,1,11,1,1,11,111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));

        MovebankEventDto result = movebankEventService.findById(1L);

        assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2011,1,11,1,1,11,111000000));
        assertThat(result.getLocationLat()).isEqualTo(11.1111d);
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(movebankEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movebankEventService.findById(99L));
    }

    @Test
    void updateDatabase_oneRecordFails_othersStillSave(){
        when(movebankClient.getData(40L)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d,-12.1212d));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(new MovebankEvent(
                        LocalDateTime.of(2012,1,21,2,1,21,212000000),
                        location, "12121212", "121212121"
                ));

        movebankEventService.updateDatabase(40L);

        verify(movebankEventRepository, times(23)).save(any(MovebankEvent.class));
    }

    @Test
    void updateDatabase_oneRecordFails_returnsPartialSuccess(){
        when(movebankClient.getData(40L)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d,-12.1212d));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(new MovebankEvent(
                        LocalDateTime.of(2012,1,21,2,1,21,212000000),
                        location, "12121212", "121212121"
                ));

        String result = movebankEventService.updateDatabase(40L);

        assertThat(result).isEqualTo("PARTIAL_SUCCESS");
    }
    @Test
    void updateDatabase_over20PercentFail_returnsFailure(){
        when(movebankClient.getData(40L)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(false);
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d,-12.1212d));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(new MovebankEvent(
                        LocalDateTime.of(2012,1,21,2,1,21,212000000),
                        location, "12121212", "121212121"
                ));

        String result = movebankEventService.updateDatabase(40L);

        assertThat(result).isEqualTo("FAILURE");
    }

    @Test
    void updateDatabase_allRecordsFail_returnsFailure(){
        when(movebankClient.getData(40L)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        when(movebankEventMapper.toEntity(any()))
                .thenThrow(new RuntimeException("Simulated failure"));
        String result = movebankEventService.updateDatabase(40L);
        assertThat(result).isEqualTo("FAILURE");
    }

    @Test
    void updateDatabase_duplicateRecords_areSkipped(){
        when(movebankClient.getData(40L)).thenReturn(loadCsvAsString("MultiRecords.csv"));
        Point point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(-22.2222, 22.2222));
        MovebankEvent entity = new MovebankEvent(LocalDateTime.now(), point, "22222222", "222222222");
        when(movebankEventMapper.toEntity(any())).thenReturn(entity);
        when(movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                any(), any(), any(), any())).thenReturn(true);

        movebankEventService.updateDatabase(40L);

        verify(movebankEventRepository, never()).save(any(MovebankEvent.class));
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