package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Route;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.RouteEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RouteMapper {
    Route toDomain(RouteEntity entity);
    RouteEntity toEntity(Route domain);
}
