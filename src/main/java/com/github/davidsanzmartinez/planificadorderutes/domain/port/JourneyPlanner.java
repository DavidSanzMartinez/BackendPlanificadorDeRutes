package com.github.davidsanzmartinez.planificadorderutes.domain.port;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;

import java.time.LocalDate;
import java.util.List;

public interface JourneyPlanner {
    List<Journey> findJourneys(String originStopId, String destinationStopId, LocalDate date, String departureTime);
}
