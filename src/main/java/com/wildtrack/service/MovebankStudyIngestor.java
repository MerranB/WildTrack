package com.wildtrack.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.MovebankHeaderNormalizer;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.repository.MovebankEventBatchWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class MovebankStudyIngestor {

    private static final int MAX_INVALID_WARNINGS = 20;
    public enum Result {FULL_SUCCESS, PARTIAL_SUCCESS, FAILURE, NO_VALID_DATA}
    private static final int WINDOW_SIZE = 20_000;
    private static final int BATCH_COUNT = 20;
    private static final Logger log = LoggerFactory.getLogger(MovebankStudyIngestor.class);
    private static final String BOM = "\uFEFF";

    private final MovebankClient movebankClient;
    private final MovebankHeaderNormalizer movebankHeaderNormalizer;
    private final MovebankEventBatchWriter movebankEventBatchWriter;

    public Result ingestStudy(Long studyId) {

        Path csv = movebankClient.getData(studyId);
        if (csv == null) {
            log.error("Study {}: data failed to be retrieved", studyId);
            return Result.FAILURE;
        }

        IngestCounters counters = new IngestCounters();

        try (InputStream body = Files.newInputStream(csv);
             Reader reader = normalizedReader(studyId, body)) {

            Iterator<MovebankEventDto> rows = new CsvToBeanBuilder<MovebankEventDto>(reader)
                    .withIgnoreEmptyLine(true)
                    .withType(MovebankEventDto.class)
                    .build()
                    .iterator();

            List<MovebankEventDto> window = new ArrayList<>(WINDOW_SIZE);

            while (rows.hasNext()) {
                MovebankEventDto row = rows.next();
                int rowNumber = counters.parsed.incrementAndGet();

                List<String> nulls = checkForNulls(row);
                if (!nulls.isEmpty()) {
                    if (counters.dropped.incrementAndGet() <= MAX_INVALID_WARNINGS) {
                        log.warn("Study {} data point {} is missing fields: {}", studyId, rowNumber, nulls);
                    }
                    continue;
                }

                window.add(row);

                if (window.size() >= WINDOW_SIZE) {
                    processBatches(window, studyId, counters);
                    window = new ArrayList<>(WINDOW_SIZE);
                }
            }

            if (!window.isEmpty()) {
                processBatches(window, studyId, counters);
            }

        } catch (IOException e) {
            log.error("Study {}: failed reading downloaded CSV", studyId, e);
            return Result.FAILURE;
        } finally {
            try {
                Files.deleteIfExists(csv);
            } catch (IOException e) {
                log.warn("Study {}: could not delete temp file {}", studyId, csv, e);
            }
        }
        counters.logSummary(studyId);
        return classify(counters);
    }

    private void processBatches(List<MovebankEventDto> data, Long studyId,
                                IngestCounters counters) {

        int batchSize = (data.size() < BATCH_COUNT) ? data.size() : (data.size() / BATCH_COUNT);
        List<Future<?>> batchResults = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < data.size(); i += batchSize) {
                int batchStart = i;
                batchResults.add(executor.submit(() -> {
                    List<MovebankEventDto> batch = data.subList(batchStart, Math.min(batchStart + batchSize, data.size()));
                    processBatch(batch, studyId, counters);
                }));
            }
        }

        for (Future<?> batchResult : batchResults) {
            try {
                batchResult.get();
            } catch (ExecutionException e) {
                log.error("StudyId {} batch died", studyId, e);
                counters.failedThreads.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("StudyId {} interrupted", studyId, e);
                counters.failedThreads.incrementAndGet();
                break;
            }
        }
    }

    private void processBatch(List<MovebankEventDto> batch, Long studyId, IngestCounters counters) {
        try {
            int inserted = movebankEventBatchWriter.insertBatch(batch);
            counters.saved.addAndGet(inserted);
            counters.dups.addAndGet(batch.size() - inserted);
        } catch (Exception e) {
            counters.failedRecords.addAndGet(batch.size());
            log.error("Study {} insert batch of {} failed (first tag {}, timestamp {})",
                    studyId, batch.size(), batch.getFirst().getTagId(),
                    batch.getFirst().getTimestamp(), e);
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

    private Reader normalizedReader(Long studyId, InputStream body) throws IOException {
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        int b;
        while ((b = body.read()) != -1 && b != '\n') {
            headerBytes.write(b);
        }

        String header = headerBytes.toString(StandardCharsets.UTF_8).replace("\r", "");
        if (header.startsWith(BOM)) {
            header = header.substring(BOM.length());
        }
        if (header.isBlank()) {
            throw new IOException("Study " + studyId + ": downloaded CSV was empty");
        }

        log.debug("Study {} header line: {}", studyId, header);
        String normalized = movebankHeaderNormalizer.normalizeHeaders(header) + "\n";

        return new InputStreamReader(
                new SequenceInputStream(
                        new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8)),
                        body),
                StandardCharsets.UTF_8);
    }

    Result classify(IngestCounters counters) {
        int usable = counters.usable();
        if (usable == 0) {
            return Result.NO_VALID_DATA;
        }
        if (counters.failedRecords.get() == 0 && counters.failedThreads.get() == 0) {
            return Result.FULL_SUCCESS;
        }
        if (((double) (usable - counters.failedRecords.get()) / usable >= .8)
                && counters.failedThreads.get() == 0) {
            return Result.PARTIAL_SUCCESS;
        }
        return Result.FAILURE;
    }

    static final class IngestCounters {
        final AtomicInteger parsed = new AtomicInteger();
        final AtomicInteger dropped = new AtomicInteger();
        final AtomicInteger saved = new AtomicInteger();
        final AtomicInteger dups = new AtomicInteger();
        final AtomicInteger failedRecords = new AtomicInteger();
        final AtomicInteger failedThreads = new AtomicInteger();

        int usable() {
            return parsed.get() - dropped.get();
        }

        void logSummary(Long studyId) {

            log.info("Ingestion complete for {}: {} parsed, {} dropped as invalid, {} saved, "
                            + "{} skipped as duplicates, {} records and {} threads failed",
                    studyId, parsed.get(), dropped.get(), saved.get(), dups.get(),
                    failedRecords.get(), failedThreads.get());
        }
    }
}