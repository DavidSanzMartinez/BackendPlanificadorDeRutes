package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.CalendarDateEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.CalendarDateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarDateJpaRepository extends JpaRepository<CalendarDateEntity, CalendarDateId> {

    List<CalendarDateEntity> findByIdServiceId(String serviceId);

    List<CalendarDateEntity> findByIdDate(LocalDate date);
}
