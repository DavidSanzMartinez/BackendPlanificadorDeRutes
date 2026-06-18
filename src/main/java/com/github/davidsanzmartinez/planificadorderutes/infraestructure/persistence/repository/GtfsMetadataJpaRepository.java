package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.GtfsMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GtfsMetadataJpaRepository extends JpaRepository<GtfsMetadataEntity, Long> {
    Optional<GtfsMetadataEntity> findByFileName(String fileName);
}
