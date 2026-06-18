package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.adapter;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.StopMapper;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.StopJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StopRepositoryAdapter implements StopRepository {
    private final StopJpaRepository jpaRepository;
    private final StopMapper mapper;

    @Override
    public List<Stop> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Stop> findById(String stopId) {
        return jpaRepository.findById(stopId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Stop> findByName(String name) {
        return jpaRepository.findByStopNameContainingIgnoreCase(name)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
