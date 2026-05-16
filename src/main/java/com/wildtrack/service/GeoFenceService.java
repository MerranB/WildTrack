package com.wildtrack.service;

import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.model.GeoFence;
import com.wildtrack.dto.GeoFenceDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wildtrack.mapper.GeoFenceMapper;
import com.wildtrack.repository.GeoFenceRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeoFenceService {


    private static final Logger log = LoggerFactory.getLogger(GeoFenceService.class);
    private final GeoFenceRepository geoFenceRepository;
    private final GeoFenceMapper geoFenceMapper;


    public Page<GeoFenceDto> findAll(Pageable pageable) {
        return geoFenceRepository.findAll(pageable)
                .map(geoFenceMapper::toDto);
    }

    public GeoFenceDto findById(Long id) {
        return geoFenceRepository.findById(id)
                .map(geoFenceMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("GeoFence", id));
    }

    @Transactional
    public GeoFenceDto update(Long id, GeoFenceDto dto) {
        GeoFence existing = geoFenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GeoFence", id));
        geoFenceMapper.updateEntityFromDto(dto, existing);
        return geoFenceMapper.toDto(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!geoFenceRepository.existsById(id)) {
            throw new ResourceNotFoundException("GeoFence", id);
        }
        geoFenceRepository.deleteById(id);
    }
    
    @Transactional
    public GeoFenceDto create(GeoFenceDto dto) {
        GeoFence saved = geoFenceRepository.save(geoFenceMapper.toEntity(dto));
        return geoFenceMapper.toDto(saved);
    }

}