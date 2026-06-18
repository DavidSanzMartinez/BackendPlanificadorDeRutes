package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.IntermediateStopDto;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.IntermediateStop;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IntermediateStopMapper {
    IntermediateStopDto toDto(IntermediateStop intermediateStop);
}
