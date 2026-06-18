package com.github.davidsanzmartinez.planificadorderutes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    private String tripId;
    private String routeId;
    private String serviceId;
    private String tripHeadsign;
    private int wheelchairAccessible;
    private String blockId;
}
