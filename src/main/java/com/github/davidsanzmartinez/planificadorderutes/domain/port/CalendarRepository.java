package com.github.davidsanzmartinez.planificadorderutes.domain.port;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Calendar;

import java.util.List;
import java.util.Optional;

public interface CalendarRepository {
    List<Calendar> findAll();
    Optional<Calendar> findByServiceId(String serviceId);
}
