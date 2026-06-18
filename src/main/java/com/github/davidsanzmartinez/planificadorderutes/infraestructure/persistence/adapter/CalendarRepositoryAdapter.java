package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.adapter;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Calendar;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.CalendarRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.CalendarMapper;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.CalendarJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CalendarRepositoryAdapter implements CalendarRepository {
    private final CalendarJpaRepository jpaRepository;
    private final CalendarMapper mapper;

    @Override
    public List<Calendar> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Calendar> findByServiceId(String serviceId) {
        return jpaRepository.findById(serviceId)
                .map(mapper::toDomain);
    }
}
