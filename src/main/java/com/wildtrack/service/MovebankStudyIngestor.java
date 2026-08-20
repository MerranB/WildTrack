package com.wildtrack.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.MovebankHeaderNormalizer;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
import com.wildtrack.repository.MovebankEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovebankStudyIngestor {

    public enum Result { FULL_SUCCESS, PARTIAL_SUCCESS, FAILURE, NO_VALID_DATA; }

    private static final int BATCH_COUNT = 20;
    private static final Logger log = LoggerFactory.getLogger(MovebankStudyIngestor.class);
    private static final String BOM = "\uFEFF";
    private final MovebankClient movebankclient;
    private final MovebankHeaderNormalizer movebankHeaderNormalizer;
    private final MovebankEventRepository movebankEventRepository;
    private final MovebankEventMapper movebankEventMapper;

    public Result ingestStudy(Long studyId) {

        String totalData = movebankclient.getData(studyId);
        if (totalData == null || totalData.isEmpty()) {
            log.error("Study {}: data failed to be retrieved", studyId);
            return Result.FAILURE;
        }

        totalData = normalizeHeaders(totalData);

        List<MovebankEventDto> data = new CsvToBeanBuilder<MovebankEventDto>(Reader.of(totalData))
                .withIgnoreEmptyLine(true)
                .withType(MovebankEventDto.class)
                .build()
                .parse();

        for (int i = 0; i < data.size(); i++) {
            List<String> nulls = checkForNulls(data.get(i));
            if (!nulls.isEmpty()) {
                log.warn("Study {} data point {} is missing fields: {}", studyId, i, nulls);
            }
        }

        data = data.stream()
                .filter(e ->
                        e.getTimestamp() != null
                        && e.getLocationLat() != null && e.getLocationLong() != null
                        && e.getTagId() != null && !e.getTagId().isBlank()
                        && e.getIndividualId() != null && !e.getIndividualId().isBlank())
                .toList();

        if(data.isEmpty()){
            return Result.NO_VALID_DATA;
        }
        return processBatches(data, studyId);
    }

    private String normalizeHeaders(String totalData) {
        if (totalData.startsWith(BOM)) {
            totalData = totalData.substring(BOM.length());
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(totalData))) {
            String firstLine = reader.readLine();
            log.debug("Header line: {}", firstLine);
            firstLine = movebankHeaderNormalizer.normalizeHeaders(firstLine);
            totalData = firstLine + "\n" + reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("error: ", e);
        }
        return totalData;
    }

    private Result processBatches(List<MovebankEventDto> data, Long studyId) {
        AtomicInteger failedRecords = new AtomicInteger();
        AtomicInteger failedThreads = new AtomicInteger();
        int batchSize = (data.size() < BATCH_COUNT) ? data.size() : (data.size() / BATCH_COUNT);
        AtomicInteger saved = new AtomicInteger();
        AtomicInteger dups = new AtomicInteger();
        List<Future<?>> batchResults = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < data.size(); i += batchSize) {
                int batchStart = i;
                batchResults.add(executor.submit(() -> {
                            List<MovebankEventDto> batch = data.subList(batchStart, Math.min(batchStart + batchSize, data.size()));
                            processBatch(batch, saved, dups, failedRecords, studyId);
                    }));
            }
        }

        for (int i = 0; i < batchResults.size(); i++) {
            try {
                batchResults.get(i).get();
            } catch (ExecutionException e) {
                log.error("StudyId {} batch died at {} ", studyId, i * batchSize, e);
                failedThreads.getAndIncrement();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("StudyId {} interrupted", studyId, e);
                failedThreads.getAndIncrement();
                break;
            }
        }

        log.info("Ingestion complete for {}: {} records parsed, {} saved, {} skipped as duplicates, and {} records and {} threads failed",
                studyId, data.size(), saved, dups, failedRecords.get(), failedThreads.get());

        if (failedRecords.get() == 0 && failedThreads.get() == 0) {
            return Result.FULL_SUCCESS;
        }
        // 80% success threshold — if more than 20% of records fail, it likely indicates
        // a systemic issue (e.g. DB connectivity) rather than isolated record failures.
        else if (((double) (data.size() - failedRecords.get()) / data.size() >= .8)
        && failedThreads.get() < 1) {
            return Result.PARTIAL_SUCCESS;
        } else {
            return Result.FAILURE;
        }
    }

    private void processBatch(List<MovebankEventDto> batch, AtomicInteger saved, AtomicInteger dups, AtomicInteger failedRecords
    , Long studyId) {
        for (MovebankEventDto dataPointDTO : batch) {
            try {
                MovebankEvent dataPoint = movebankEventMapper.toEntity(dataPointDTO);
                if (!movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId(
                        dataPoint.getTimestamp(), dataPoint.getLocation(),
                        dataPoint.getIndividualId(), dataPoint.getTagId())) {
                    movebankEventRepository.save(dataPoint);
                    saved.getAndIncrement();
                } else {
                    dups.getAndIncrement();
                }
            } catch (DataIntegrityViolationException _) {
                dups.getAndIncrement();
            }
            catch (Exception e) {
                failedRecords.getAndIncrement();
                log.error("Study {} record failed (tag {}, timestamp {})",
                        studyId, dataPointDTO.getTagId(), dataPointDTO.getTimestamp(), e);

            }
        }
    }

    private List<String> checkForNulls(MovebankEventDto dataPoint) {
        List<String> nullVariables = new ArrayList<>();
        if (dataPoint.getTimestamp() == null) {
            nullVariables.add("timestamp");
        }
        if (dataPoint.getLocationLat() == null) {
            nullVariables.add("location_lat");
        }
        if (dataPoint.getLocationLong() == null) {
            nullVariables.add("location_long");
        }
        if (dataPoint.getIndividualId() == null || dataPoint.getIndividualId().isBlank()) {
            nullVariables.add("individual_id");
        }
        if (dataPoint.getTagId() == null || dataPoint.getTagId().isBlank()) {
            nullVariables.add("tag_id");
        }
        return nullVariables;
    }
}
