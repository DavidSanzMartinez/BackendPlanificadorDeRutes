package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Calendar;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.CalendarEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CalendarMapper {
    Calendar toDomain(CalendarEntity serviceId);
    CalendarEntity toEntity(Calendar domain);
}
