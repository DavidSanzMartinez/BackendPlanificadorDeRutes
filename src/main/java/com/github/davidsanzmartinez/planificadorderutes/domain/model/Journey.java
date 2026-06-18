package com.github.davidsanzmartinez.planificadorderutes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journey {
    private Stop origin;
    private Stop destination;
    private String totalDuration;
    private String departureTime;
    private String arrivalTime;
    private int numTransfers;
    private List<Connection> connections;
    private boolean hasRealtimeData;
    private Double transferReliability;
    private Double expectedDelayMinutes;
}
