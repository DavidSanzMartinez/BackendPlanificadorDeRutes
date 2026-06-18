package com.github.davidsanzmartinez.planificadorderutes.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JourneyRequestDto {
    @NotBlank
    private String originStopId;
    @NotBlank
    private String destinationStopId;
    @NotNull
    private LocalDate date;
    @NotBlank
    private String departureTime;

    private String algorithm;
}
