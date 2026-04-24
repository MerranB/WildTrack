package com.wildtrack.mapper;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.model.MovebankEvent;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MovebankEventMapper {

    MovebankEventDto toDto(MovebankEvent movebankevent);

    MovebankEventDto toEntity(MovebankEventDto movebankeventdto);
}