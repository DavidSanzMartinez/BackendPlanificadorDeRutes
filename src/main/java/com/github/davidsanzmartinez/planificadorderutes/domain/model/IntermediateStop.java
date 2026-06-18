package com.github.davidsanzmartinez.planificadorderutes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntermediateStop {
    private String stopId;
    private String stopName;
    private double lat;
    private double lon;
    private String arrivalTime;
    private String departureTime;
}
