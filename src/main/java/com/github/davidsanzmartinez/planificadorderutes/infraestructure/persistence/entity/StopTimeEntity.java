package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stop_times", indexes = {
        @Index(name = "idx_stop_times_trip_id", columnList = "trip_id, stop_sequence"),
        @Index(name = "idx_stop_times_stop_id", columnList = "stop_id, departure_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopTimeEntity {

    @EmbeddedId
    private StopTimeId id;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Column(name = "arrival_time")
    private String arrivalTime;

    @Column(name = "departure_time")
    private String departureTime;

    @Column(name = "pickup_type")
    private Integer pickupType;

    @Column(name = "drop_off_type")
    private Integer dropOffType;
}
