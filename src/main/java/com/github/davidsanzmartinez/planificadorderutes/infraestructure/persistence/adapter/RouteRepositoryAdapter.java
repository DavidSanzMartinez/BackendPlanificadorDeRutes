package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.adapter;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Route;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.RouteRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.RouteMapper;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.RouteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RouteRepositoryAdapter implements RouteRepository {
    private final RouteJpaRepository jpaRepository;
    private final RouteMapper mapper;

    @Override
    public List<Route> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Route> findById(String routeId) {
        return jpaRepository.findById(routeId)
                .map(mapper::toDomain);
    }
}
