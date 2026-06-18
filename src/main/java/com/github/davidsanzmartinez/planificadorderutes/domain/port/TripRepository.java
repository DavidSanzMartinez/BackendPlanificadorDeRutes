package com.github.davidsanzmartinez.planificadorderutes.domain.port;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Trip;

import java.util.List;
import java.util.Optional;

public interface TripRepository {
    List<Trip> findAll();
    Optional<Trip> findById(String tripId);
    List<Trip> findByServiceId(String serviceId);
}
