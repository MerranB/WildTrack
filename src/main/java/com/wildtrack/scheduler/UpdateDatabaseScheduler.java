package com.wildtrack.scheduler;

import com.wildtrack.model.MovebankStudy;
import com.wildtrack.repository.MovebankStudyRepository;
import com.wildtrack.service.MovebankEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@ConditionalOnProperty(
        name = "scheduler.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Component
public class UpdateDatabaseScheduler {

    private final MovebankStudyRepository movebankStudyRepository;
    private final MovebankEventService movebankEventService;
    private static final Logger log = LoggerFactory.getLogger(UpdateDatabaseScheduler.class);

    @Scheduled(cron = "${scheduler.cronTime}")
    public void updateAllStudies() {
        List<MovebankStudy> studies = movebankStudyRepository.findAll();
        for(MovebankStudy study: studies){
            if(study.getId()!=null) {
                movebankEventService.updateDatabase(study.getId());
            }
        }
    }
}