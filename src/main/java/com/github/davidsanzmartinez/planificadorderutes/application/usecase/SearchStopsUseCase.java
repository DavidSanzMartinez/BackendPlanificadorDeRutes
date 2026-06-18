package com.github.davidsanzmartinez.planificadorderutes.application.usecase;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchStopsUseCase {

    private final StopRepository stopRepository;

    public List<Stop> execute(String query) {
        return stopRepository.findByName(query);
    }
}
