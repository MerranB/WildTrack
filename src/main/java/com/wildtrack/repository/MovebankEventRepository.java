package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import org.locationtech.jts.geom.Point;

@Repository
public interface MovebankEventRepository extends JpaRepository<MovebankEvent, Long> {

    boolean existsByTimestampAndLocationAndIndividualIdAndTagId(
            LocalDateTime timestamp, Point location,
            String individualId, String tagId);
}