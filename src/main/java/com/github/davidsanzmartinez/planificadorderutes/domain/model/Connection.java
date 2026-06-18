package com.github.davidsanzmartinez.planificadorderutes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Connection {
    private String tripId;
    private String departureStopId;
    private String arrivalStopId;
    private String departureTime;
    private String arrivalTime;
    private int departureSequence;
    private int arrivalSequence;
    private Stop departureStop;
    private Stop arrivalStop;
    private Integer walkingMinutes;
    private String routeShortName;
    private String routeColor;
    private List<IntermediateStop> intermediateStops;
    private Integer realtimeDelayMinutes;
    private Boolean cancelled;
}
