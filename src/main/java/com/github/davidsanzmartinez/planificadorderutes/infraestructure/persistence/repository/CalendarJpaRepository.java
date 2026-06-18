package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.CalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarJpaRepository extends JpaRepository<CalendarEntity, String> {
}
