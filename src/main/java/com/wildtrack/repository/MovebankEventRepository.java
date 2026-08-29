package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MovebankEventRepository extends JpaRepository<MovebankEvent, Long> {

    interface HotspotProjection {
        Double getLat();
        Double getLon();
        Long getTotal();
    }

    Slice<MovebankEvent> findAllBy(Pageable pageable);

@Query(nativeQuery = true, value = "SELECT * FROM movebank_event WHERE  ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :range)",
    countQuery = "SELECT COUNT(*) FROM movebank_event WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :range)")
    Page<MovebankEvent> allDataPointsByRange(@Param("lat") double lat, @Param("lon") double lon, @Param("range") double range, Pageable pageable);

@Query(nativeQuery = true, value = "SELECT * FROM movebank_event WHERE  ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :range) AND timestamp >= :startDate AND timestamp <= :endDate",
    countQuery = "SELECT COUNT(*) FROM movebank_event WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :range) AND timestamp >= :startDate AND timestamp <= :endDate")
    Page<MovebankEvent> allDataPointsByRangeAndTime(@Param("lat") double lat, @Param("lon") double lon, @Param("range") double range, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);


@Query(
    nativeQuery = true,
    value = "SELECT * FROM movebank_event WHERE ST_Within(location, ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326))",
    countQuery = "SELECT COUNT(*) FROM movebank_event WHERE ST_Within(location, ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326))")
    Page<MovebankEvent> allDataPointsByBox(@Param("minLon") double minLon, @Param("minLat") double minLat, @Param("maxLon") double maxLon, @Param("maxLat") double maxLat, Pageable pageable);

@Query(
    nativeQuery = true,
    value = "SELECT COUNT(*) FROM (SELECT DISTINCT ON (individual_id) * FROM movebank_event ORDER BY individual_id, timestamp DESC) latest_events WHERE ST_Within(location, :area)")
    int countAnimalsInGeofence(@Param("area") Polygon area);

@Query(nativeQuery = true, value = """
              WITH tile AS (
                  SELECT ST_TileEnvelope(:z, :x, :y) AS mercator,
                         ST_Transform(
                             ST_Intersection(
                                 ST_TileEnvelope(:z, :x, :y, margin => (64.0 / 4096)),
                                 ST_TileEnvelope(0, 0, 0)
                         ),
                         4326
                     ) AS geographic
              ),
              mvtgeom AS (
                  SELECT ST_AsMVTGeom(
                             ST_Transform(e.location, 3857),
                             tile.mercator,
                             extent => 4096,
                             buffer => 64
                         ) AS geom,
                         e.individual_id,
                         e.tag_id,
                         1 AS point_count
                  FROM movebank_event e, tile
                  WHERE e.location && tile.geographic
              )
              SELECT ST_AsMVT(mvtgeom.*, 'events', 4096, 'geom')
              FROM mvtgeom
              WHERE geom IS NOT NULL
              """)
    byte[] findRawTile(@Param("z") int z, @Param("x") int x, @Param("y") int y);

@Query(nativeQuery = true, value = """
        WITH tile AS (
                            SELECT ST_TileEnvelope(:z, :x, :y) AS mercator,
                                   ST_Transform(
                                       ST_Intersection(
                                           ST_TileEnvelope(:z, :x, :y, margin => (64.0 / 4096)),
                                           ST_TileEnvelope(0, 0, 0)
                                       ),
                                       4326
                                   ) AS geographic,
                                   (360.0 / power(2, :z) / :cells) AS cell_size
                        ),
              clustered AS (
                  SELECT ST_SnapToGrid(e.location, tile.cell_size) AS cell,
                         count(*) AS point_count
                  FROM movebank_event e, tile
                  WHERE e.location && tile.geographic
                  GROUP BY ST_SnapToGrid(e.location, tile.cell_size)
              ),
              mvtgeom AS (
                  SELECT ST_AsMVTGeom(
                             ST_Transform(clustered.cell, 3857),
                             tile.mercator,
                             extent => 4096,
                             buffer => 64
                         ) AS geom,
                         clustered.point_count
                  FROM clustered, tile
              )
              SELECT ST_AsMVT(mvtgeom.*, 'events', 4096, 'geom')
              FROM mvtgeom
              WHERE geom IS NOT NULL
              """)
    byte[] findClusteredTile(@Param("z") int z, @Param("x") int x,
                             @Param("y") int y, @Param("cells") int cells);

    @Query(nativeQuery = true, value = """
              SELECT avg(ST_Y(location)) AS lat,
                     avg(ST_X(location)) AS lon,
                     count(*) AS total
              FROM movebank_event
              GROUP BY ST_SnapToGrid(location, :gridSize)
              ORDER BY total DESC
              LIMIT :maxCells
              """)
    List<HotspotProjection> findHotspots(@Param("gridSize") double gridSize,
                                         @Param("maxCells") int maxCells);

}
