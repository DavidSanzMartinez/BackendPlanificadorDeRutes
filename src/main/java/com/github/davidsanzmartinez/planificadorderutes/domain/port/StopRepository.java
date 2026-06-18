package com.github.davidsanzmartinez.planificadorderutes.domain.port;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;

import java.util.List;
import java.util.Optional;

public interface StopRepository {
    List<Stop> findAll();
    Optional<Stop> findById(String stopId);
    List<Stop> findByName(String stopName);
}
