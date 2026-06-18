package com.github.davidsanzmartinez.planificadorderutes.api.controller;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.StopDto;
import com.github.davidsanzmartinez.planificadorderutes.application.usecase.SearchStopsUseCase;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.StopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
@RequiredArgsConstructor
public class StopController {
    private final SearchStopsUseCase searchStopsUseCase;
    private final StopMapper stopMapper;

    @GetMapping
    public ResponseEntity<List<StopDto>> searchStops(@RequestParam String query) {
        List<Stop> stops = searchStopsUseCase.execute(query);
        List<StopDto> dtos = stops.stream().map(stopMapper::toDto).toList();
        return ResponseEntity.ok(dtos);
    }
}
