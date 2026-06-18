package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopEntity {

    @Id
    @Column(name = "stop_id")
    private String stopId;

    @Column(name= "stop_name", nullable = false)
    private String stopName;

    @Column(name= "stop_lat", nullable = false)
    private double stopLat;

    @Column(name= "stop_lon", nullable = false)
    private double stopLon;

    @Column(name= "wheelchair_boarding")
    private int wheelchairBoarding;
}
