package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StopTimeId {

    @Column(name = "trip_id")
    private String tripId;
    @Column(name = "stop_sequence")
    private int stopSequence;
}
