package com.wildtrack.scheduler;

import com.wildtrack.model.MovebankStudy;
import com.wildtrack.repository.MovebankStudyRepository;
import com.wildtrack.service.MovebankEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import java.lang.reflect.Method;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

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
    void updateAllStudies_callsUpdateDatabase_forEachStudy() {
        MovebankStudy study1 = new MovebankStudy();
        study1.setId(1L);
        MovebankStudy study2 = new MovebankStudy();
        study2.setId(2L);
        when(movebankStudyRepository.findAll()).thenReturn(List.of(study1, study2));

        scheduler.updateAllStudies();

        verify(movebankEventService).updateDatabase(1L);
        verify(movebankEventService).updateDatabase(2L);
    }

    @Test
    void updateAllStudies_skipsStudy_whenIdIsNull() {
        MovebankStudy study = new MovebankStudy();
        study.setId(null);
        when(movebankStudyRepository.findAll()).thenReturn(List.of(study));

        scheduler.updateAllStudies();

        verify(movebankEventService, never()).updateDatabase(any());
    }
}