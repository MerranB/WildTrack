package com.wildtrack.repository;

import com.wildtrack.model.MovebankEvent;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MovebankEventRepositoryTest {

    @Autowired
    private MovebankEventRepository repository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Point point(double lon, double lat) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }

    @Test
    void allDataPointsByRange_includesEventsWithinRadius() {
        MovebankEvent inside  = new MovebankEvent(LocalDateTime.now(), point(0.001, 0.001), "inside",  "tag1");
        MovebankEvent outside = new MovebankEvent(LocalDateTime.now(), point(5.0,   5.0),   "outside", "tag2");
        repository.saveAll(List.of(inside, outside));

        Page<MovebankEvent> result = repository.allDataPointsByRange(0.0, 0.0, 0.1, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIndividualId()).isEqualTo("inside");
    }

    @Test
    void allDataPointsByRange_excludesEventsOutsideRadius() {
        MovebankEvent outside1 = new MovebankEvent(LocalDateTime.now(), point(2.0,  2.0),  "outside1", "tag1");
        MovebankEvent outside2 = new MovebankEvent(LocalDateTime.now(), point(-3.0, -3.0), "outside2", "tag2");
        repository.saveAll(List.of(outside1, outside2));

        Page<MovebankEvent> result = repository.allDataPointsByRange(0.0, 0.0, 0.1, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void allDataPointsByRangeAndTime_includesEventsWithinRadiusAndDateRange() {
        MovebankEvent inside     = new MovebankEvent(LocalDateTime.of(2015, 6, 15, 0, 0), point(0.001, 0.001), "inside",      "tag1");
        MovebankEvent outsideDate = new MovebankEvent(LocalDateTime.of(2013, 6, 15, 0, 0), point(0.001, 0.001), "outsideDate", "tag2");
        repository.saveAll(List.of(inside, outsideDate));

        Page<MovebankEvent> result = repository.allDataPointsByRangeAndTime(
                0.0, 0.0, 0.1,
                LocalDate.of(2015, 1, 1), LocalDate.of(2015, 12, 31),
                Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIndividualId()).isEqualTo("inside");
    }

    @Test
    void allDataPointsByRangeAndTime_excludesEventsOutsideDateRange() {
        MovebankEvent beforeRange = new MovebankEvent(LocalDateTime.of(2013, 12, 31, 0, 0), point(0.001, 0.001), "beforeRange", "tag1");
        MovebankEvent afterRange  = new MovebankEvent(LocalDateTime.of(2017, 1,  1,  0, 0), point(0.001, 0.001), "afterRange",  "tag2");
        repository.saveAll(List.of(beforeRange, afterRange));

        Page<MovebankEvent> result = repository.allDataPointsByRangeAndTime(
                0.0, 0.0, 0.1,
                LocalDate.of(2015, 1, 1), LocalDate.of(2015, 12, 31),
                Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void allDataPointsByBox_includesEventsWithinBoundingBox() {
        MovebankEvent inside  = new MovebankEvent(LocalDateTime.now(), point(0.5, 0.5), "inside",  "tag1");
        MovebankEvent outside = new MovebankEvent(LocalDateTime.now(), point(2.0, 2.0), "outside", "tag2");
        repository.saveAll(List.of(inside, outside));

        Page<MovebankEvent> result = repository.allDataPointsByBox(-1.0, -1.0, 1.0, 1.0, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIndividualId()).isEqualTo("inside");
    }

    @Test
    void allDataPointsByBox_excludesEventsOutsideBoundingBox() {
        MovebankEvent outside1 = new MovebankEvent(LocalDateTime.now(), point(2.0,  2.0),  "outside1", "tag1");
        MovebankEvent outside2 = new MovebankEvent(LocalDateTime.now(), point(-2.0, -2.0), "outside2", "tag2");
        repository.saveAll(List.of(outside1, outside2));

        Page<MovebankEvent> result = repository.allDataPointsByBox(-1.0, -1.0, 1.0, 1.0, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }
}
