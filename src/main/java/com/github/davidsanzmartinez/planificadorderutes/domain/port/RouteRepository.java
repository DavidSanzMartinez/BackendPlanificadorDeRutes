package com.github.davidsanzmartinez.planificadorderutes.domain.port;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Route;

import java.util.List;
import java.util.Optional;

public interface RouteRepository {
    List<Route> findAll();
    Optional<Route> findById(String routeId);
}
