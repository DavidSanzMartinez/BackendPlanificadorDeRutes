package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.adapter;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.StopTime;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopTimeRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.StopTimeMapper;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.StopTimeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StopTimeRepositoryAdapter implements StopTimeRepository {
    private final StopTimeJpaRepository jpaRepository;
    private final StopTimeMapper mapper;

    @Override
    public List<StopTime> findByTripId(String tripId) {
        return jpaRepository.findByIdTripIdOrderByIdStopSequence(tripId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<StopTime> findAll() {
        return jpaRepository.findAllOrderedByDepartureTime()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
