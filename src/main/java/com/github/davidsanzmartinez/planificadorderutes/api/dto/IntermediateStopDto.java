package com.github.davidsanzmartinez.planificadorderutes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntermediateStopDto {
    private String stopId;
    private String stopName;
    private double lat;
    private double lon;
    private String arrivalTime;
    private String departureTime;
}