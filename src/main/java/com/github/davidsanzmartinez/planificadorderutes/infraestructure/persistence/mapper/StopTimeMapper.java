package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.StopTime;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopTimeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StopTimeMapper {
    @Mapping(source = "id.tripId", target = "tripId")
    @Mapping(source = "id.stopSequence", target = "stopSequence")
    StopTime toDomain(StopTimeEntity entity);

    @Mapping(source = "tripId", target = "id.tripId")
    @Mapping(source = "stopSequence", target = "id.stopSequence")
    StopTimeEntity toEntity(StopTime domain);
}
