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
@Table(name = "trips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripEntity {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "trip_headsign")
    private String tripHeadsign;

    @Column(name = "wheelchair_accessible")
    private int wheelchairAccessible;

    @Column(name = "block_id")
    private String blockId;
}
