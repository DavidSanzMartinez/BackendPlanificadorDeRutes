package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Trip;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.TripEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripMapper {
    Trip toDomain(TripEntity entity);
    TripEntity toEntity(Trip domain);
}
