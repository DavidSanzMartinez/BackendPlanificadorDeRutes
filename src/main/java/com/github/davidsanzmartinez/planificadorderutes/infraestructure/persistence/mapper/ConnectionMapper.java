package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.ConnectionDto;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StopMapper.class, IntermediateStopMapper.class})
public interface ConnectionMapper {
    ConnectionDto toDto(Connection connection);
}
