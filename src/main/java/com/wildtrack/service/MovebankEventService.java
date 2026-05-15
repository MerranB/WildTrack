package com.wildtrack.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.model.MovebankEvent;
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

        List<MovebankEventDto> data = new CsvToBeanBuilder<MovebankEventDto>(Reader.of(movebankclient.getData(id)))
                .withIgnoreEmptyLine(true)
                .withType(MovebankEventDto.class)
                .build()
                .parse();
        for(int i = 0; i < data.size(); i ++){
            List<String> nulls =  checkForNulls(data.get(i));
            if(!nulls.isEmpty()){
                log.warn("Data Point {} is missing fields: {}", i, nulls);
            }

        }

        return  processBatches(data).toString();
    }

    public Page<MovebankEventDto> allDataPointsByRange(double lat, double lon, double range, Pageable pageable){
        return movebankEventRepository.allDataPointsByRange(lat, lon, range, pageable)
        .map(movebankEventMapper::toDto);
    }

    public Page<MovebankEventDto> allDataPointsByBox(double minLon, double minLat, double maxLon, double maxLat, Pageable pageable){
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

    private Result processBatches(List<MovebankEventDto> data){
        List<String> failedThreads = Collections.synchronizedList(new ArrayList<>());
        int batchSize = (data.size() < BATCH_COUNT) ? data.size() : (data.size() / BATCH_COUNT);
        AtomicInteger saved = new AtomicInteger();
        AtomicInteger dups = new AtomicInteger();

        try(var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = 0; i < data.size(); i += batchSize) {
                int batchStart = i;
                executor.submit(() -> {
                    List<MovebankEventDto> batch = data.subList(batchStart, Math.min(batchStart + batchSize, data.size()));
                    processBatch(batch, saved, dups, failedThreads);
                });
            }
        }
        log.info("Ingestion complete: {} records parsed, {} saved, {} skipped as duplicates, and {} failed",
                data.size(),saved, dups, failedThreads.size());

        if(!failedThreads.isEmpty()){
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

    private void processBatch(List<MovebankEventDto> batch,AtomicInteger saved,AtomicInteger dups,  List<String> failedThreads){

        for (MovebankEventDto dataPointDTO : batch) {
            try {
                MovebankEvent dataPoint = movebankEventMapper.toEntity(dataPointDTO);
                if (!movebankEventRepository.existsByTimestampAndLocationAndIndividualIdAndTagId
                        (dataPoint.getTimestamp(), dataPoint.getLocation(),
                                dataPoint.getIndividualId(), dataPoint.getTagId()))
                {
                    movebankEventRepository.save(dataPoint);
                    saved.getAndIncrement();
                }
                else{
                    dups.getAndIncrement();
                }
            }
            catch(Exception e){
                failedThreads.add(e.toString());
            }
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
        if(dataPoint.getIndividualId() == null || dataPoint.getIndividualId().isBlank()){
            nullVariables.add("individual_id");
        }
        if(dataPoint.getTagId() == null  || dataPoint.getTagId().isBlank()){
            nullVariables.add("tag_id");
        }
        return nullVariables;
    }
}