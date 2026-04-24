package com.wildtrack.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.wildtrack.client.MovebankClient;
import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.MovebankEventMapper;
import com.wildtrack.repository.MovebankEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovebankIngestionService {

    private final MovebankClient Movebankclient;
    private final MovebankEventRepository movebankEventRepository;
    private final MovebankEventMapper movebankEventMapper;

    public List<MovebankEventDto>  getData(Long id) throws IOException {

        return new CsvToBeanBuilder<MovebankEventDto>(Reader.of(Movebankclient.getData(id)))
                        .withIgnoreEmptyLine(true)
                        .withType(MovebankEventDto.class)
                        .build()
                        .parse();
    }

    public Page<MovebankEventDto> findAll(Pageable pageable) {
        return movebankEventRepository.findAll(pageable)
                .map(movebankEventMapper::toDto);
    }

    public MovebankEventDto findById(Long id) {
        return movebankEventRepository.findById(id)
                .map(movebankEventMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Item", id));

    }
}