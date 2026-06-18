package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.adapter;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Trip;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.TripRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.TripMapper;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.TripJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TripRepositoryAdapter implements TripRepository {
    private final TripJpaRepository jpaRepository;
    private final TripMapper mapper;

    @Override
    public List<Trip> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Trip> findById(String tripId) {
        return jpaRepository.findById(tripId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Trip> findByServiceId(String serviceId) {
        return jpaRepository.findByServiceId(serviceId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
