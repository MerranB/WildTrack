package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MovebankEventRepository extends JpaRepository<MovebankEvent, Long> {

    boolean existsByTimestampAndLocationLatAndLocationLongAndIndividualIdAndTagId(
            LocalDateTime timestamp, Double locationLat, Double locationLong,
            String individualId, String tagId);
}
