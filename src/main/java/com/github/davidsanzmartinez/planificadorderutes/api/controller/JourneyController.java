package com.github.davidsanzmartinez.planificadorderutes.api.controller;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.JourneyDto;
import com.github.davidsanzmartinez.planificadorderutes.api.dto.JourneyRequestDto;
import com.github.davidsanzmartinez.planificadorderutes.application.usecase.GetJourneyDetailsUseCase;
import com.github.davidsanzmartinez.planificadorderutes.application.usecase.SearchJourneyUseCase;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.mapper.JourneyMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/journeys")
@RequiredArgsConstructor
public class JourneyController {
    private final SearchJourneyUseCase searchJourneyUseCase;
    private final GetJourneyDetailsUseCase getJourneyDetailsUseCase;
    private final JourneyMapper journeyMapper;

    @PostMapping("/search")
    public ResponseEntity<List<JourneyDto>> searchJourneys(
            @RequestBody @Valid JourneyRequestDto request) {
        List<Journey> journeys = searchJourneyUseCase.execute(
                request.getOriginStopId(),
                request.getDestinationStopId(),
                request.getDate(),
                request.getDepartureTime(),
                request.getAlgorithm()
        );
        List<JourneyDto> dtos = journeys.stream()
                .map(journeyMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{journeyId}")
    public ResponseEntity<JourneyDto> getJourneyDetails(@PathVariable String journeyId) {
        return ResponseEntity.ok().build();
    }
}
