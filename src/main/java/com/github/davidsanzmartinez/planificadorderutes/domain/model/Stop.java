package com.github.davidsanzmartinez.planificadorderutes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stop {
    private String stopId;
    private String stopName;
    private double stopLat;
    private double stopLon;
    private int wheelchairBoarding;
}
