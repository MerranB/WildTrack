package com.wildtrack.service;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.config.MovebankProperties;
import com.wildtrack.dto.Hotspot;
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
import org.springframework.data.domain.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
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

    private static final int RAW_MIN_ZOOM = 11;
    private static final int CELLS_PER_TILE = 64;
    private static final double GRID_SIZE = 10.0;
    private static final int MAX_CELLS = 30;

    @BeforeEach
    void setUpPoint() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        ReflectionTestUtils.setField(movebankEventService, "rawTileMinZoom", RAW_MIN_ZOOM);
        ReflectionTestUtils.setField(movebankEventService, "cellsPerTile", CELLS_PER_TILE);
        ReflectionTestUtils.setField(movebankEventService, "hotspotGridSize", GRID_SIZE);
        ReflectionTestUtils.setField(movebankEventService, "hotspotMaxCells", MAX_CELLS);
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
    void findAll_returnsSliceOfDtos() {
        when(movebankEventMapper.toDto(any())).thenReturn(new MovebankEventDto(
                LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                11.1111d, -11.1111d, "11111111", "111111111"
        ));
        Point location = geometryFactory.createPoint(new Coordinate(12.1212d, -12.1212d));
        when(movebankEventRepository.findAllBy(any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(new MovebankEvent(
                        LocalDateTime.of(2011, 1, 11, 1, 1, 11, 111000000),
                        location, "11111111", "111111111"
                ))));

        Slice<MovebankEventDto> result = movebankEventService.findAll(Pageable.unpaged());

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

    @Test
    void getTileByZ_usesRawQuery_atRawMinZoom() {
        when(movebankEventRepository.findRawTile(RAW_MIN_ZOOM, 3, 4)).thenReturn(new byte[]{1});

        byte[] tile = movebankEventService.getTileByZ(RAW_MIN_ZOOM, 3, 4);

        assertThat(tile).containsExactly(1);
        verify(movebankEventRepository).findRawTile(RAW_MIN_ZOOM, 3, 4);
        verify(movebankEventRepository, never()).findClusteredTile(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void getTileByZ_usesRawQuery_aboveRawMinZoom() {
        when(movebankEventRepository.findRawTile(RAW_MIN_ZOOM + 3, 3, 4)).thenReturn(new byte[]{1});

        movebankEventService.getTileByZ(RAW_MIN_ZOOM + 3, 3, 4);

        verify(movebankEventRepository).findRawTile(RAW_MIN_ZOOM + 3, 3, 4);
        verify(movebankEventRepository, never()).findClusteredTile(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void getTileByZ_usesClusteredQueryWithConfiguredCellCount_belowRawMinZoom() {
        when(movebankEventRepository.findClusteredTile(RAW_MIN_ZOOM - 1, 3, 4, CELLS_PER_TILE))
                .thenReturn(new byte[]{2});

        byte[] tile = movebankEventService.getTileByZ(RAW_MIN_ZOOM - 1, 3, 4);

        assertThat(tile).containsExactly(2);
        verify(movebankEventRepository).findClusteredTile(RAW_MIN_ZOOM - 1, 3, 4, CELLS_PER_TILE);
        verify(movebankEventRepository, never()).findRawTile(anyInt(), anyInt(), anyInt());
    }

    @Test
    void getTileByZ_usesClusteredQuery_atWorldZoom() {
        when(movebankEventRepository.findClusteredTile(0, 0, 0, CELLS_PER_TILE)).thenReturn(new byte[]{2});

        movebankEventService.getTileByZ(0, 0, 0);

        verify(movebankEventRepository).findClusteredTile(0, 0, 0, CELLS_PER_TILE);
    }

    @Test
    void getTileByZ_passesEmptyTileThroughForEmptyRegion() {
        when(movebankEventRepository.findRawTile(RAW_MIN_ZOOM, 0, 0)).thenReturn(new byte[0]);

        assertThat(movebankEventService.getTileByZ(RAW_MIN_ZOOM, 0, 0)).isEmpty();
    }

    @Test
    void getTileByZ_passesNullThrough() {
        when(movebankEventRepository.findRawTile(RAW_MIN_ZOOM, 0, 0)).thenReturn(null);

        assertThat(movebankEventService.getTileByZ(RAW_MIN_ZOOM, 0, 0)).isNull();
    }

    @Test
    void hotspots_mapsProjectionsToDtosWithConfiguredGrid() {
        MovebankEventRepository.HotspotProjection first = projection(37.41, -6.43, 2538111L);
        MovebankEventRepository.HotspotProjection second = projection(33.19, -117.52, 38050L);

        when(movebankEventRepository.findHotspots(GRID_SIZE, MAX_CELLS))
                .thenReturn(List.of(first, second));

        List<Hotspot> result = movebankEventService.hotspots();

        assertThat(result).containsExactly(
                new Hotspot(37.41, -6.43, 2538111L),
                new Hotspot(33.19, -117.52, 38050L));
        verify(movebankEventRepository).findHotspots(GRID_SIZE, MAX_CELLS);
    }

    @Test
    void hotspots_returnsEmptyList_whenNoData() {
        when(movebankEventRepository.findHotspots(GRID_SIZE, MAX_CELLS)).thenReturn(List.of());

        assertThat(movebankEventService.hotspots()).isEmpty();
    }

    @Test
    void getTileByZ_propagatesRepositoryFailure_onRawTile() {
        when(movebankEventRepository.findRawTile(RAW_MIN_ZOOM, 1, 1))
                .thenThrow(new DataAccessResourceFailureException("connection lost"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> movebankEventService.getTileByZ(RAW_MIN_ZOOM, 1, 1));
    }

    @Test
    void getTileByZ_propagatesRepositoryFailure_onClusteredTile() {
        when(movebankEventRepository.findClusteredTile(0, 0, 0, CELLS_PER_TILE))
                .thenThrow(new DataAccessResourceFailureException("connection lost"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> movebankEventService.getTileByZ(0, 0, 0));
    }

    @Test
    void hotspots_propagatesRepositoryFailure() {
        when(movebankEventRepository.findHotspots(GRID_SIZE, MAX_CELLS))
                .thenThrow(new DataAccessResourceFailureException("connection lost"));

        assertThrows(DataAccessResourceFailureException.class, () -> movebankEventService.hotspots());
    }

    @Test
    void hotspots_throwsIfProjectionReturnsNulls() {
        MovebankEventRepository.HotspotProjection broken =
                org.mockito.Mockito.mock(MovebankEventRepository.HotspotProjection.class);
        when(broken.getLat()).thenReturn(null);
        when(movebankEventRepository.findHotspots(GRID_SIZE, MAX_CELLS)).thenReturn(List.of(broken));

        assertThrows(NullPointerException.class, () -> movebankEventService.hotspots());
    }

    private MovebankEventRepository.HotspotProjection projection(double lat, double lon, long total) {
        MovebankEventRepository.HotspotProjection cell =
                org.mockito.Mockito.mock(MovebankEventRepository.HotspotProjection.class);
        when(cell.getLat()).thenReturn(lat);
        when(cell.getLon()).thenReturn(lon);
        when(cell.getTotal()).thenReturn(total);
        return cell;
    }
}
