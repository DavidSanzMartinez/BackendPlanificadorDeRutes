package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.StopDto;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StopMapper {
    Stop toDomain(StopEntity entity);
    StopEntity toEntity(Stop domain);

    @Mapping(source = "stopLat", target = "lat")
    @Mapping(source = "stopLon", target = "lon")
    StopDto toDto(Stop stop);
}
