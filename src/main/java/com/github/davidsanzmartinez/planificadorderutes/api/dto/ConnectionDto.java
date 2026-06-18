package com.github.davidsanzmartinez.planificadorderutes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDto {
    private String tripId;
    private String departureStopId;
    private String arrivalStopId;
    private String departureTime;
    private String arrivalTime;
    private StopDto departureStop;
    private StopDto arrivalStop;
    private Integer walkingMinutes;
    private String routeShortName;
    private String routeColor;
    private List<IntermediateStopDto> intermediateStops;
    private Integer realtimeDelayMinutes;
    private Boolean cancelled;
}
