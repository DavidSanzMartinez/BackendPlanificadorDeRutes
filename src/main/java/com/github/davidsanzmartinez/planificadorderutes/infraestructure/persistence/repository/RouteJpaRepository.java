package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteJpaRepository extends JpaRepository<RouteEntity, String> {
}
