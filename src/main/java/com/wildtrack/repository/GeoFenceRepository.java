package com.wildtrack.repository;

import com.wildtrack.model.GeoFence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoFenceRepository extends JpaRepository<GeoFence, Long> {

    boolean existsByName(
            String name);
}