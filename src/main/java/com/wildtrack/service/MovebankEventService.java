package com.wildtrack.service;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.config.MovebankProperties;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.repository.MovebankEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovebankEventService {

    private static final Logger log = LoggerFactory.getLogger(MovebankEventService.class);

    private final MovebankEventRepository movebankEventRepository;
    private final MovebankEventMapper movebankEventMapper;
    private final MovebankStudyIngestor movebankStudyIngestor;
    private final MovebankProperties movebankProperties;

    // No DB transaction held during the (slow) HTTP loop
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateDatabase() {
        List<Long> studyIds = movebankProperties.getStudyIds();
        if (studyIds == null || studyIds.isEmpty()) {
            return "FAILED - No study IDs configured";
        }

        StringBuilder summary = new StringBuilder();
        for (Long studyId : studyIds) {
            try {
                summary.append(movebankStudyIngestor.ingestStudy(studyId)).append(" for ").append(studyId).append("\n");
            } catch (Exception e) {
                log.error("Study {} ingestion failed", studyId, e);
                summary.append("Study ").append(studyId)
                       .append(" FAILED - ").append(e.getMessage()).append("\n");
            }
        }
        return summary.toString();
    }

    public Page<MovebankEventDto> allDataPointsByRange(double lat, double lon, double range, Pageable pageable) {
        return movebankEventRepository.allDataPointsByRange(lat, lon, range, pageable)
                .map(movebankEventMapper::toDto);
    }

    public Page<MovebankEventDto> allDataPointsByBox(double minLon, double minLat, double maxLon, double maxLat, Pageable pageable) {
        return movebankEventRepository.allDataPointsByBox(minLon, minLat, maxLon, maxLat, pageable)
                .map(movebankEventMapper::toDto);
    }

    public Page<MovebankEventDto> findAll(Pageable pageable) {
        return movebankEventRepository.findAll(pageable)
                .map(movebankEventMapper::toDto);
    }

    public MovebankEventDto findById(Long id) {
        return movebankEventRepository.findById(id)
                .map(movebankEventMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MovebankEvent", id));
    }

    @Transactional
    public void delete(Long id) {
        if (!movebankEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Movebank", id);
        }
        movebankEventRepository.deleteById(id);
    }
}
