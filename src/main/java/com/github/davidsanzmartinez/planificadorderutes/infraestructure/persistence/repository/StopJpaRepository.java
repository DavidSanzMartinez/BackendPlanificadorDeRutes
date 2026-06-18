package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopJpaRepository extends JpaRepository<StopEntity, String> {
    List<StopEntity> findByStopNameContainingIgnoreCase(String name);
}
