package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MovebankEventRepositoryTest {

    @Autowired
    private MovebankEventRepository repository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Point point(double lon, double lat) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }

    @Test
    void allDataPointsByRange_includesEventsWithinRadius() {
        MovebankEvent inside  = new MovebankEvent(LocalDateTime.now(), point(0.001, 0.001), "inside",  "tag1");
        MovebankEvent outside = new MovebankEvent(LocalDateTime.now(), point(5.0,   5.0),   "outside", "tag2");
        repository.saveAll(List.of(inside, outside));

        Page<MovebankEvent> result = repository.allDataPointsByRange(0.0, 0.0, 0.1, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIndividualId()).isEqualTo("inside");
    }

    @Test
    void allDataPointsByRange_excludesEventsOutsideRadius() {
        MovebankEvent outside1 = new MovebankEvent(LocalDateTime.now(), point(2.0,  2.0),  "outside1", "tag1");
        MovebankEvent outside2 = new MovebankEvent(LocalDateTime.now(), point(-3.0, -3.0), "outside2", "tag2");
        repository.saveAll(List.of(outside1, outside2));

        Page<MovebankEvent> result = repository.allDataPointsByRange(0.0, 0.0, 0.1, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void allDataPointsByRangeAndTime_includesEventsWithinRadiusAndDateRange() {
        MovebankEvent inside     = new MovebankEvent(LocalDateTime.of(2015, 6, 15, 0, 0), point(0.001, 0.001), "inside",      "tag1");
        MovebankEvent outsideDate = new MovebankEvent(LocalDateTime.of(2013, 6, 15, 0, 0), point(0.001, 0.001), "outsideDate", "tag2");
        repository.saveAll(List.of(inside, outsideDate));

        Page<MovebankEvent> result = repository.allDataPointsByRangeAndTime(
                0.0, 0.0, 0.1,
                LocalDate.of(2015, 1, 1), LocalDate.of(2015, 12, 31),
                Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIndividualId()).isEqualTo("inside");
    }

    @Test
    void allDataPointsByRangeAndTime_excludesEventsOutsideDateRange() {
        MovebankEvent beforeRange = new MovebankEvent(LocalDateTime.of(2013, 12, 31, 0, 0), point(0.001, 0.001), "beforeRange", "tag1");
        MovebankEvent afterRange  = new MovebankEvent(LocalDateTime.of(2017, 1,  1,  0, 0), point(0.001, 0.001), "afterRange",  "tag2");
        repository.saveAll(List.of(beforeRange, afterRange));

        Page<MovebankEvent> result = repository.allDataPointsByRangeAndTime(
                0.0, 0.0, 0.1,
                LocalDate.of(2015, 1, 1), LocalDate.of(2015, 12, 31),
                Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void allDataPointsByBox_includesEventsWithinBoundingBox() {
        MovebankEvent inside  = new MovebankEvent(LocalDateTime.now(), point(0.5, 0.5), "inside",  "tag1");
        MovebankEvent outside = new MovebankEvent(LocalDateTime.now(), point(2.0, 2.0), "outside", "tag2");
        repository.saveAll(List.of(inside, outside));

        Page<MovebankEvent> result = repository.allDataPointsByBox(-1.0, -1.0, 1.0, 1.0, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIndividualId()).isEqualTo("inside");
    }

    @Test
    void allDataPointsByBox_excludesEventsOutsideBoundingBox() {
        MovebankEvent outside1 = new MovebankEvent(LocalDateTime.now(), point(2.0,  2.0),  "outside1", "tag1");
        MovebankEvent outside2 = new MovebankEvent(LocalDateTime.now(), point(-2.0, -2.0), "outside2", "tag2");
        repository.saveAll(List.of(outside1, outside2));

        Page<MovebankEvent> result = repository.allDataPointsByBox(-1.0, -1.0, 1.0, 1.0, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    private static final int ZOOM = 4;
    private static final int TILE_X = 8;
    private static final int TILE_Y = 7;
    private static final int EMPTY_TILE_X = 4;
    private static final int CELLS_PER_TILE = 64;
    private List<MovebankEvent> denseCluster(int count) {
        List<MovebankEvent> events = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            events.add(new MovebankEvent(
                    LocalDateTime.of(2020, 1, 1, 0, 0).plusMinutes(i),
                    point(10.0 + i * 0.0001, 10.0 + i * 0.0001),
                    "animal" + i, "tag" + i));
        }
        return events;
    }

    @Test
    void findRawTile_returnsTileForRegionContainingEvents() {
        repository.saveAllAndFlush(denseCluster(5));

        byte[] tile = repository.findRawTile(ZOOM, TILE_X, TILE_Y);

        assertThat(tile).isNotNull().isNotEmpty();
    }

    @Test
    void findRawTile_returnsEmptyTileForRegionWithNoEvents() {
        repository.saveAllAndFlush(denseCluster(5));

        byte[] tile = repository.findRawTile(ZOOM, EMPTY_TILE_X, TILE_Y);

        assertThat(tile).isEmpty();
    }

    @Test
    void findClusteredTile_returnsTileForRegionContainingEvents() {
        repository.saveAllAndFlush(denseCluster(5));

        byte[] tile = repository.findClusteredTile(ZOOM, TILE_X, TILE_Y, CELLS_PER_TILE);

        assertThat(tile).isNotNull().isNotEmpty();
    }

    @Test
    void findClusteredTile_returnsEmptyTileForRegionWithNoEvents() {
        repository.saveAllAndFlush(denseCluster(5));

        byte[] tile = repository.findClusteredTile(ZOOM, EMPTY_TILE_X, TILE_Y, CELLS_PER_TILE);

        assertThat(tile).isEmpty();
    }
    @Test
    void findClusteredTile_isSmallerThanRawTileForTheSameDenseRegion() {
        repository.saveAllAndFlush(denseCluster(200));

        byte[] raw = repository.findRawTile(ZOOM, TILE_X, TILE_Y);
        byte[] clustered = repository.findClusteredTile(ZOOM, TILE_X, TILE_Y, CELLS_PER_TILE);

        assertThat(clustered).hasSizeLessThan(raw.length);
    }
    private static final int ANTIMERIDIAN_ZOOM = 1;
    private static final int EAST_TILE_X = 1;
    private static final int WEST_TILE_X = 0;
    private static final int NORTH_TILE_Y = 0;

    private MovebankEvent eventAt(double lon, double lat, String id) {
        return new MovebankEvent(LocalDateTime.of(2021, 3, 4, 5, 6), point(lon, lat), id, "tag-" + id);
    }

    @Test
    void findRawTile_placesEasternPointInEasternmostTile() {
        repository.saveAllAndFlush(List.of(eventAt(10.0, 10.0, "east")));

        assertThat(repository.findRawTile(ANTIMERIDIAN_ZOOM, EAST_TILE_X, NORTH_TILE_Y)).isNotEmpty();
        assertThat(repository.findRawTile(ANTIMERIDIAN_ZOOM, WEST_TILE_X, NORTH_TILE_Y)).isEmpty();
    }

    @Test
    void findRawTile_placesWesternPointInWesternmostTile() {
        repository.saveAllAndFlush(List.of(eventAt(-64.6, 18.4, "west")));

        assertThat(repository.findRawTile(ANTIMERIDIAN_ZOOM, WEST_TILE_X, NORTH_TILE_Y)).isNotEmpty();
        assertThat(repository.findRawTile(ANTIMERIDIAN_ZOOM, EAST_TILE_X, NORTH_TILE_Y)).isEmpty();
    }

    @Test
    void findClusteredTile_placesEasternPointInEasternmostTile() {
        repository.saveAllAndFlush(List.of(eventAt(10.0, 10.0, "east")));

        assertThat(repository.findClusteredTile(ANTIMERIDIAN_ZOOM, EAST_TILE_X, NORTH_TILE_Y, CELLS_PER_TILE)).isNotEmpty();
        assertThat(repository.findClusteredTile(ANTIMERIDIAN_ZOOM, WEST_TILE_X, NORTH_TILE_Y, CELLS_PER_TILE)).isEmpty();
    }

    @Test
    void findClusteredTile_placesWesternPointInWesternmostTile() {
        repository.saveAllAndFlush(List.of(eventAt(-64.6, 18.4, "west")));

        assertThat(repository.findClusteredTile(ANTIMERIDIAN_ZOOM, WEST_TILE_X, NORTH_TILE_Y, CELLS_PER_TILE)).isNotEmpty();
        assertThat(repository.findClusteredTile(ANTIMERIDIAN_ZOOM, EAST_TILE_X, NORTH_TILE_Y, CELLS_PER_TILE)).isEmpty();
    }

    @Test
    void findRawTile_throwsForTileIndexOutsideZoomGrid() {
        repository.saveAllAndFlush(denseCluster(5));

        assertThatThrownBy(() -> repository.findRawTile(ZOOM, 9999, 9999))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void findHotspots_returnsSingleCell_whenAllEventsShareOneGridCell() {
        repository.saveAllAndFlush(denseCluster(5));

        List<MovebankEventRepository.HotspotProjection> cells = repository.findHotspots(10.0, 30);

        assertThat(cells).hasSize(1);
        assertThat(cells.getFirst().getTotal()).isEqualTo(5L);
    }
    @Test
    void findHotspots_zeroGridSizeDegradesToOneCellPerDistinctLocation() {
        repository.saveAllAndFlush(denseCluster(5));

        List<MovebankEventRepository.HotspotProjection> cells = repository.findHotspots(0.0, 30);

        assertThat(cells).hasSize(5).allSatisfy(cell -> assertThat(cell.getTotal()).isEqualTo(1L));
    }

    @Test
    void findHotspots_groupsEventsIntoCellsWithCounts() {
        List<MovebankEvent> events = new ArrayList<>(denseCluster(5));
        events.add(new MovebankEvent(LocalDateTime.of(2020, 2, 1, 0, 0), point(50.0, 50.0), "far1", "tagA"));
        events.add(new MovebankEvent(LocalDateTime.of(2020, 2, 2, 0, 0), point(50.1, 50.1), "far2", "tagB"));
        repository.saveAllAndFlush(events);

        List<MovebankEventRepository.HotspotProjection> cells = repository.findHotspots(10.0, 30);

        assertThat(cells).hasSize(2);
        assertThat(cells.getFirst().getTotal()).isEqualTo(5L);
        assertThat(cells.getFirst().getLat()).isCloseTo(10.0, within(0.01));
        assertThat(cells.getFirst().getLon()).isCloseTo(10.0, within(0.01));
        assertThat(cells.get(1).getTotal()).isEqualTo(2L);
    }

    @Test
    void findHotspots_limitsResultsToMaxCells() {
        List<MovebankEvent> events = new ArrayList<>(denseCluster(5));
        events.add(new MovebankEvent(LocalDateTime.of(2020, 2, 1, 0, 0), point(50.0, 50.0), "far1", "tagA"));
        repository.saveAllAndFlush(events);

        List<MovebankEventRepository.HotspotProjection> cells = repository.findHotspots(10.0, 1);

        assertThat(cells).hasSize(1);
        assertThat(cells.getFirst().getTotal()).isEqualTo(5L);
    }

    @Test
    void findHotspots_returnsEmptyWhenNoEvents() {
        assertThat(repository.findHotspots(10.0, 30)).isEmpty();
    }
}
