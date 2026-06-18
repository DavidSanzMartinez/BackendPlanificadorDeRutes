package com.github.davidsanzmartinez.planificadorderutes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopDto {
    private String stopId;
    private String stopName;
    private double lat;
    private double lon;
    private int wheelchairBoarding;
}
