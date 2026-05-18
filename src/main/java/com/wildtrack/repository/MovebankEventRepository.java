package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MovebankEventRepository extends JpaRepository<MovebankEvent, Long> {

@Query(nativeQuery = true, value = "SELECT * FROM movebank_event WHERE  ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :range)",
    countQuery = "SELECT COUNT(*) FROM movebank_event WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :range)")
    Page<MovebankEvent> allDataPointsByRange(@Param("lat") double lat, @Param("lon") double lon, @Param("range") double range, Pageable pageable);

@Query(
    nativeQuery = true,
    value = "SELECT * FROM movebank_event WHERE ST_Within(location, ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326))",
    countQuery = "SELECT COUNT(*) FROM movebank_event WHERE ST_Within(location, ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326))")
    Page<MovebankEvent> allDataPointsByBox(@Param("minLon") double minLon, @Param("minLat") double minLat, @Param("maxLon") double maxLon, @Param("maxLat") double maxLat, Pageable pageable);

@Query(
    nativeQuery = true,
    value = "SELECT COUNT(*) FROM (SELECT DISTINCT ON (individual_id) * FROM movebank_event ORDER BY individual_id, timestamp DESC) latest_events WHERE ST_Within(location, :area)")
    int countAnimalsInGeofence(@Param("area") Polygon area);

    boolean existsByTimestampAndLocationAndIndividualIdAndTagId(
            LocalDateTime timestamp, Point location,
            String individualId, String tagId);
}
