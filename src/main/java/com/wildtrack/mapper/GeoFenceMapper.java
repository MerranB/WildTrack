package com.wildtrack.mapper;

import org.mapstruct.*;
import com.wildtrack.dto.CoordinateDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import java.util.List;
import java.util.Arrays;
import com.wildtrack.model.GeoFence;
import com.wildtrack.dto.GeoFenceDto;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface GeoFenceMapper {
    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "area", expression = "java(coordinatesToPolygon(geoFenceDto.getCoordinates()))")
    void updateEntityFromDto(GeoFenceDto geoFenceDto, @MappingTarget GeoFence geoFence);

    @Mapping(target = "coordinates", expression = "java(polygonToCoordinates(geoFence.getArea()))")
    GeoFenceDto toDto(GeoFence geoFence);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "area", expression = "java(coordinatesToPolygon(geoFenceDto.getCoordinates()))")
    GeoFence toEntity(GeoFenceDto geoFenceDto);

    @SuppressWarnings("squid:S1168")
    default List<CoordinateDto> polygonToCoordinates(Polygon polygon) {
    if (polygon == null) return null;
    return Arrays.stream(polygon.getCoordinates())
            .map(cord -> new CoordinateDto(cord.y, cord.x)).toList();
    }

    default Polygon coordinatesToPolygon(List<CoordinateDto> coordinates) {
    if (coordinates == null) return null;
    Coordinate[] coords = coordinates.stream()
            .map(c -> new Coordinate(c.lon(), c.lat()))
            .toArray(Coordinate[]::new);
    return GEOMETRY_FACTORY.createPolygon(coords);
}

}