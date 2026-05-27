package com.wildtrack.scheduler;

import com.wildtrack.service.MovebankEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(
        name = "scheduler.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Component
public class UpdateDatabaseScheduler {

    private final MovebankEventService movebankEventService;
    private static final Logger log = LoggerFactory.getLogger(UpdateDatabaseScheduler.class);

    @Scheduled(cron = "${scheduler.cronTime.updateDatabase}")
    public void updateAllStudies() {
        log.info("Nightly ingestion result: {}", movebankEventService.updateDatabase());
       }
    }