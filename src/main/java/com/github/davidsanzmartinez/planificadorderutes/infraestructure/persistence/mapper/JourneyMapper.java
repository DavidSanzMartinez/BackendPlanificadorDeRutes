package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.JourneyDto;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StopMapper.class, ConnectionMapper.class})
public interface JourneyMapper {
    JourneyDto toDto(Journey journey);
}
