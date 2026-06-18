package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.TransferTimeEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.TransferTimeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferTimeJpaRepository extends JpaRepository<TransferTimeEntity, TransferTimeId> {
    List<TransferTimeEntity> findByIdFromStopId(String fromStopId);
    List<TransferTimeEntity> findByIdFromStopIdAndIdFromRouteId(String fromStopId, String fromRouteId);
}
