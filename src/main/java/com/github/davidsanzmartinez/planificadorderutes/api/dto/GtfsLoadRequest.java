package com.github.davidsanzmartinez.planificadorderutes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsLoadRequest {
    private String directory1;
    private String directory2;
}
