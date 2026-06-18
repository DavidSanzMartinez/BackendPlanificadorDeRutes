package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "footpaths")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FootpathEntity {

    @EmbeddedId
    private FootpathId id;

    @Column(name = "transfer_time", nullable = false)
    private int transferTime;

    @Column(name = "distance_meters")
    private Double distanceMeters;
}