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
        // Creates a list to see if threads fail and why.
        List<String> failedThreads = Collections.synchronizedList(new ArrayList<>());

        // This splits up the data into 20 batches (And if the total is less than 20, then the batch is that.)
        int batchSize = (data.size() < BATCH_COUNT) ? data.size() : (data.size() / BATCH_COUNT);

        // Start of thread
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Set up the threads to run in batched.  All the batches run at once but, in the batch
            // ,it runs one check at a time
            for (int i = 0; i < data.size(); i += batchSize) {
                int finalI = i;
                executor.submit(() -> {
                        // Creates a sub list of the data for each batch.
                        List<MovebankEventDto> batch = data.subList(finalI, Math.min(finalI + batchSize, data.size()));

                        // This is the meat of the code. It first checks if the data already exists in the database
                        // that is the existsBy (That really long method name.).  Then, if it is unique, it will put
                        // it in the database, else it will skip it.
                        for (MovebankEventDto dataPoint : batch) {
                            // The try catch is to track what threads are failing, and what the error message is.
                            try {

                            if (!movebankEventRepository.existsByTimestampAndLocationLatAndLocationLongAndIndividualIdAndTagId
                                    (dataPoint.getTimestamp(), dataPoint.getLocationLat(),
                                     dataPoint.getLocationLong(), dataPoint.getIndividualId(), dataPoint.getTagId()))
                            {
                                movebankEventRepository.save(movebankEventMapper.toEntity(dataPoint));
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
        // So cause of threads, if I put this earlier, it will get lost in the console, so I need to put it outside
        //  the threads.  This will tell me if something went wrong.
        if(!failedThreads.isEmpty()){
            // TODO: Configure Logback file appender to write ERROR logs to a rolling file —
            // console output will truncate at high failure volumes (2000+ errors at 20% failure rate)
            log.error("{} threads failed to run!", failedThreads.size());
            failedThreads.forEach(log::error);
        }

        if(failedThreads.isEmpty()){
            return Result.FULL_SUCCESS;
        }
        else if(((double) (data.size() - failedThreads.size()) / data.size() >= .8)){
            return Result.PARTIAL_SUCCESS;
        }
        else{
            return Result.FAILURE;
        }
    }
}