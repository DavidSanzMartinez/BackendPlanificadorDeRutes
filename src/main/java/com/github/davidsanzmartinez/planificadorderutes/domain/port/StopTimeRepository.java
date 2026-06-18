package com.github.davidsanzmartinez.planificadorderutes.domain.port;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.StopTime;

import java.util.List;

public interface StopTimeRepository {
    List<StopTime> findAll();
    List<StopTime> findByTripId(String tripId);
}
