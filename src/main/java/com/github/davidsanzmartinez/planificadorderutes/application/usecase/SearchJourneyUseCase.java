package com.github.davidsanzmartinez.planificadorderutes.application.usecase;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.JourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing.CsaMeatJourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing.DijkstraJourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing.McRaptorJourneyPlanner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchJourneyUseCase {

    private final DijkstraJourneyPlanner dijkstra;
    private final CsaMeatJourneyPlanner csaMeat;
    private final McRaptorJourneyPlanner mcRaptor;
    private final StopRepository stopRepository;
    private final GetJourneyDetailsUseCase getJourneyDetailsUseCase;

    public List<Journey> execute(String originStopId, String destinationStopId,
                                 LocalDate date, String departureTime, String algorithm) {
        JourneyPlanner planner = selectPlanner(algorithm);
        List<Journey> journeys = planner.findJourneys(
                originStopId, destinationStopId, date, departureTime);

        Stop origin = stopRepository.findById(originStopId).orElse(null);
        Stop destination = stopRepository.findById(destinationStopId).orElse(null);

        return journeys.stream()
                .map(j -> {
                    Journey enriched = getJourneyDetailsUseCase.execute(j);
                    return Journey.builder()
                            .origin(origin)
                            .destination(destination)
                            .departureTime(enriched.getDepartureTime())
                            .arrivalTime(enriched.getArrivalTime())
                            .totalDuration(enriched.getTotalDuration())
                            .numTransfers(enriched.getNumTransfers())
                            .connections(enriched.getConnections())
                            .transferReliability(enriched.getTransferReliability())
                            .expectedDelayMinutes(enriched.getExpectedDelayMinutes())
                            .hasRealtimeData(enriched.isHasRealtimeData())
                            .build();
                })
                .toList();
    }

    private JourneyPlanner selectPlanner(String algorithm) {
        if (algorithm == null) return mcRaptor;  // default
        return switch (algorithm.toLowerCase()) {
            case "csa-meat" -> csaMeat;
            case "dijkstra" -> dijkstra;
            default -> mcRaptor;
        };
    }
}