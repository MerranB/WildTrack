package com.wildtrack.mapper;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.model.MovebankEvent;
import org.mapstruct.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MovebankEventMapper {
    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "locationLat", source = "location", qualifiedByName = "pointToLat")
    @Mapping(target = "locationLong", source = "location", qualifiedByName = "pointToLong")
    MovebankEventDto toDto(MovebankEvent movebankevent);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", expression = "java(toPoint(movebankeventdto.getLocationLat(), movebankeventdto.getLocationLong()))")
    MovebankEvent toEntity(MovebankEventDto movebankeventdto);

    @Named("pointToLat")
    default Double pointToLat(Point point) {
        return point == null ? null : point.getY();
    }

    @Named("pointToLong")
    default Double pointToLong(Point point) {
        return point == null ? null : point.getX();
    }

    default Point toPoint(Double lat, Double lon) {
        if (lat == null || lon == null) return null;
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }
}