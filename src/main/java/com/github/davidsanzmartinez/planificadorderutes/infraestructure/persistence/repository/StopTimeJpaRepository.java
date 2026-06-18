package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopTimeEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopTimeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopTimeJpaRepository extends JpaRepository<StopTimeEntity, StopTimeId> {
    List<StopTimeEntity> findByIdTripIdOrderByIdStopSequence(String tripId);

    @Query("SELECT s FROM StopTimeEntity s ORDER BY s.departureTime ASC")
    List<StopTimeEntity> findAllOrderedByDepartureTime();
}
