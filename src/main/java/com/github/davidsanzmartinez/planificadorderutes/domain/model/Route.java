package com.github.davidsanzmartinez.planificadorderutes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    private String routeId;
    private String routeShortName;
    private String routeLongName;
    private int routeType;
    private String routeColor;
    private String routeTextColor;
}
