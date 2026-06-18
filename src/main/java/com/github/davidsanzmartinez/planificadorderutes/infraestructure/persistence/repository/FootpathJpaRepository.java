package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.FootpathEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.FootpathId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FootpathJpaRepository extends JpaRepository<FootpathEntity, FootpathId> {
    List<FootpathEntity> findByIdFromStopId(String fromStopId);
}
