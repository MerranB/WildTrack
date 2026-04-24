package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovebankEventRepository extends JpaRepository<MovebankEvent, Long> {

}
