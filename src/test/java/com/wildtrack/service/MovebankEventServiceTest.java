package com.wildtrack.service;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.config.MovebankProperties;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import com.wildtrack.service.MovebankStudyIngestor.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovebankEventServiceTest {

    @Mock
    private MovebankEventRepository movebankEventRepository;

    @Mock
    private MovebankEventMapper movebankEventMapper;

    @Mock
    private MovebankStudyIngestor movebankStudyIngestor;

    @Mock
    private MovebankProperties movebankProperties;

    @InjectMocks
    private MovebankEventService movebankEventService;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUpPoint() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Test
    void updateDatabase_delegatesToIngestorPerStudyId() {
        when(movebankProperties.getStudyIds()).thenReturn(List.of(1L, 2L));
        when(movebankStudyIngestor.ingestStudy(1L)).thenReturn(Result.FULL_SUCCESS);
        when(movebankStudyIngestor.ingestStudy(2L)).thenReturn(Result.FULL_SUCCESS);

        String result = movebankEventService.updateDatabase();

        verify(movebankStudyIngestor).ingestStudy(1L);
        verify(movebankStudyIngestor).ingestStudy(2L);
        assertThat(result).contains("FULL_SUCCESS for 1").contains("FULL_SUCCESS for 2");
    }

    @Test
    void updateDatabase_mixedResults_summaryCarriesBoth() {
        when(movebankProperties.getStudyIds()).thenReturn(List.of(1L, 2L));
        when(movebankStudyIngestor.ingestStudy(1L)).thenReturn(Result.FULL_SUCCESS);
        when(movebankStudyIngestor.ingestStudy(2L)).thenReturn(Result.FAILURE);

        String result = movebankEventService.updateDatabase();

        assertThat(result).contains("FULL_SUCCESS for 1").contains("FAILURE for 2");
    }

    // The controller classifies by substring, so the summary must carry each study's
    // result verbatim. This pins the format the two sides agree on.
    @Test
    void updateDatabase_noValidData_summaryNamesResultAndStudy() {
        when(movebankProperties.getStudyIds()).thenReturn(List.of(1073231887L));
        when(movebankStudyIngestor.ingestStudy(1073231887L)).thenReturn(Result.NO_VALID_DATA);

        String result = movebankEventService.updateDatabase();

        assertThat(result).contains("NO_VALID_DATA for 1073231887");
    }

    @Test
    void updateDatabase_noStudyIdsConfigured_returnsMessage() {
        when(movebankProperties.getStudyIds()).thenReturn(List.of());

        String result = movebankEventService.updateDatabase();

        assertThat(result).isEqualTo("FAILED - No study IDs configured");
        verify(movebankStudyIngestor, never()).ingestStudy(anyLong());
    }

    @Test
    void updateDatabase_oneStudyFails_othersStillRun() {
        when(movebankProperties.getStudyIds()).thenReturn(List.of(1L, 2L));
        when(movebankStudyIngestor.ingestStudy(1L)).thenThrow(new RuntimeException("boom"));
        when(movebankStudyIngestor.ingestStudy(2L)).thenReturn(Result.FULL_SUCCESS);

        String result = movebankEventService.updateDatabase();

        verify(movebankStudyIngestor).ingestStudy(2L);
        assertThat(result).contains("FAILED").contains("FULL_SUCCESS for 2");
    }

    @Test
    void findAll_returnsPageOfDtos() {
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new MovebankEvent(
                        LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                        location, "11111111", "111111111"
                ))));

        Page<MovebankEventDto> result = movebankEventService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTimestamp())
                .isEqualTo(LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000));
    }

    @Test
    void findById_returnsCorrectDto() {
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        MovebankEvent entity = new MovebankEvent(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                location, "11111111", "111111111"
        );
        when(movebankEventRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));

        MovebankEventDto result = movebankEventService.findById(1L);

        assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000));
        assertThat(result.getLocationLat()).isEqualTo(11.1111d);
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(movebankEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movebankEventService.findById(99L));
    }

    @Test
    void allDataPointsByRange_returnsPageOfDtos() {
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        MovebankEvent entity = new MovebankEvent(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                location, "11111111", "111111111"
        );
        when(movebankEventRepository.allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));

        Page<MovebankEventDto> result = movebankEventService.allDataPointsByRange(0.0, 0.0, 5.0, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getLocationLat()).isEqualTo(11.1111d);
    }

    @Test
    void allDataPointsByRange_returnsEmptyPage_whenNoResults() {
        when(movebankEventRepository.allDataPointsByRange(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<MovebankEventDto> result = movebankEventService.allDataPointsByRange(0.0, 0.0, 5.0, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void allDataPointsByBox_returnsPageOfDtos() {
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        MovebankEvent entity = new MovebankEvent(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                location, "11111111", "111111111"
        );
        when(movebankEventRepository.allDataPointsByBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));

        Page<MovebankEventDto> result = movebankEventService.allDataPointsByBox(-10.0, -10.0, 10.0, 10.0, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getLocationLat()).isEqualTo(11.1111d);
    }

    @Test
    void allDataPointsByBox_returnsEmptyPage_whenNoResults() {
        when(movebankEventRepository.allDataPointsByBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<MovebankEventDto> result = movebankEventService.allDataPointsByBox(-10.0, -10.0, 10.0, 10.0, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void delete_deletesWhenFound() {
        when(movebankEventRepository.existsById(1L)).thenReturn(true);
        movebankEventService.delete(1L);
        verify(movebankEventRepository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(movebankEventRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> movebankEventService.delete(99L));
    }
}
