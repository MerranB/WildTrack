package com.wildtrack.scheduler;

import com.wildtrack.repository.MovebankStudyRepository;
import com.wildtrack.service.MovebankEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateDatabaseSchedulerTest {

    @Mock
    private MovebankStudyRepository movebankStudyRepository;

    @Mock
    private MovebankEventService movebankEventService;

    @InjectMocks
    private UpdateDatabaseScheduler scheduler;

    @Test
    void updateAllStudies_cron_reads_from_properties() throws NoSuchMethodException {
        Method method = UpdateDatabaseScheduler.class.getMethod("updateAllStudies");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${scheduler.cronTime.updateDatabase}");
    }
    @Test
    void updateAllStudies_callsUpdateDatabase() {
        scheduler.updateAllStudies();
        verify(movebankEventService).updateDatabase();
    }
}