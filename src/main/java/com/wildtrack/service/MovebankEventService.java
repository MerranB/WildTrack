package com.wildtrack.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.repository.MovebankEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovebankEventService {
    public enum Result { FULL_SUCCESS, PARTIAL_SUCCESS, FAILURE; }
    private static final int BATCH_COUNT = 20;
    private static final Logger log = LoggerFactory.getLogger(MovebankEventService.class);
    private final MovebankClient movebankclient;
    private final MovebankEventRepository movebankEventRepository;
    private final MovebankEventMapper movebankEventMapper;

    @Transactional
    public String updateDatabase(Long id) {

        // This calls another method() that pulls in data from a website. This parses the CSV into basically POJO.
        List<MovebankEventDto> data = new CsvToBeanBuilder<MovebankEventDto>(Reader.of(movebankclient.getData(id)))
                .withIgnoreEmptyLine(true)
                .withType(MovebankEventDto.class)
                .build()
                .parse();

        for(int i = 0; i < data.size(); i ++){
            List<String> nulls =  checkForNulls(data.get(i));
            if(!nulls.isEmpty()){
                log.warn("Data Point {} is missing fields: {}", i, String.join(", ", nulls));
            }
        }

        return  processBatches(data).toString();
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

    private Result processBatches(List<MovebankEventDto> data){
        List<String> failedThreads = Collections.synchronizedList(new ArrayList<>());
        int batchSize = (data.size() < BATCH_COUNT) ? data.size() : (data.size() / BATCH_COUNT);
        AtomicInteger saved = new AtomicInteger();
        AtomicInteger dups = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = 0; i < data.size(); i += batchSize) {
                int i_in_loop = i;
                executor.submit(() -> {
                    List<MovebankEventDto> batch = data.subList(i_in_loop, Math.min(i_in_loop + batchSize, data.size()));

                    for (MovebankEventDto dataPoint : batch) {
                        try {

                            if (!movebankEventRepository.existsByTimestampAndLocationLatAndLocationLongAndIndividualIdAndTagId
                                    (dataPoint.getTimestamp(), dataPoint.getLocationLat(),
                                            dataPoint.getLocationLong(), dataPoint.getIndividualId(), dataPoint.getTagId()))
                            {
                                movebankEventRepository.save(movebankEventMapper.toEntity(dataPoint));
                                saved.getAndIncrement();
                            }
                            else{
                                dups.getAndIncrement();
                            }
                        }
                        // TODO: Add error classification — transient errors (DB down) should abort remaining batches,
                        // persistent errors should be logged for investigation. Consider dead letter queue in AWS.
                        catch(Exception e){
                            failedThreads.add(e.toString());
                        }
                    }
                });
            }
        }
        catch(RejectedExecutionException e){
            log.error("Executor rejected task submission: {}",  e.getMessage());
            throw e;
        }

        log.info("Ingestion complete: {} records parsed, {} saved, {} skipped as duplicates, and {} failed",
                data.size(),saved, dups, failedThreads.size());

        if(!failedThreads.isEmpty()){
            // TODO: Configure Logback file appender to write ERROR logs to a rolling file —
            // console output will truncate at high failure volumes (2000+ errors at 20% failure rate)
            log.error("{} threads failed to run!", failedThreads.size());
            failedThreads.forEach(log::error);
        }

        if(failedThreads.isEmpty()){
            return Result.FULL_SUCCESS;
        }
        // 80% success threshold — if more than 20% of records fail, it likely indicates
        // a systemic issue (e.g. DB connectivity) rather than isolated record failures.
        else if(((double) (data.size() - failedThreads.size()) / data.size() >= .8)){
            return Result.PARTIAL_SUCCESS;
        }
        else{
            return Result.FAILURE;
        }
    }

    private List<String> checkForNulls(MovebankEventDto dataPoint){
        List<String> nullVariables = new ArrayList<>();
        if(dataPoint.getTimestamp() == null){
            nullVariables.add("timestamp");
        }
        if(dataPoint.getLocationLat() == null){
            nullVariables.add("location_lat");
        }
        if(dataPoint.getLocationLong() == null){
            nullVariables.add("location_long");
        }
        if(dataPoint.getIndividualId() == null){
            nullVariables.add("individual_id");
        }
        if(dataPoint.getTagId() == null){
            nullVariables.add("tag_id");
        }
        return nullVariables;
    }
}