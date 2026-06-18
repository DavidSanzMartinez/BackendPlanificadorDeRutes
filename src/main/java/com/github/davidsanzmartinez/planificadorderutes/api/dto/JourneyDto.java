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
public class JourneyDto {
    private StopDto origin;
    private StopDto destination;
    private String totalDuration;
    private String departureTime;
    private String arrivalTime;
    private int numTransfers;
    private List<ConnectionDto> connections;
    private boolean hasRealtimeData;
    private Double transferReliability;
    private Double expectedDelayMinutes;
}
