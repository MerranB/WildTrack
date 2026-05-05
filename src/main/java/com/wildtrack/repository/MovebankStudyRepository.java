package com.wildtrack.repository;

import com.wildtrack.model.MovebankStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovebankStudyRepository extends JpaRepository<MovebankStudy, Long> {

}

