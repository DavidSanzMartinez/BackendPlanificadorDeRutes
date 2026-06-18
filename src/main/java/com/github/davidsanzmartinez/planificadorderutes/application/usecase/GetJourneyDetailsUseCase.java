package com.github.davidsanzmartinez.planificadorderutes.application.usecase;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.IntermediateStop;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Stop;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.RouteRepository;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopRepository;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetJourneyDetailsUseCase {

    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;


    public Journey execute(Journey journey) {
        List<Connection> grouped = groupConnectionsByTrip(journey.getConnections());

        return Journey.builder()
                .origin(journey.getOrigin())
                .destination(journey.getDestination())
                .departureTime(journey.getDepartureTime())
                .arrivalTime(journey.getArrivalTime())
                .totalDuration(journey.getTotalDuration())
                .numTransfers(journey.getNumTransfers())
                .connections(grouped)
                .transferReliability(journey.getTransferReliability())
                .expectedDelayMinutes(journey.getExpectedDelayMinutes())
                .hasRealtimeData(journey.isHasRealtimeData())
                .build();
    }

    private List<Connection> groupConnectionsByTrip(List<Connection> connections) {
        List<Connection> result = new ArrayList<>();
        int i = 0;

        while (i < connections.size()) {
            Connection first = connections.get(i);
            String currentTripId = first.getTripId();

            // WALK no se agrupa
            if ("WALK".equals(currentTripId)) {
                Stop departure = stopRepository.findById(first.getDepartureStopId()).orElse(null);
                Stop arrival = stopRepository.findById(first.getArrivalStopId()).orElse(null);
                result.add(first.toBuilder()
                        .departureStop(departure)
                        .arrivalStop(arrival)
                        .intermediateStops(List.of())
                        .build());
                i++;
                continue;
            }

            // Agrupa todas las conexiones consecutivas del mismo tripId
            List<Connection> group = new ArrayList<>();
            group.add(first);
            while (i + 1 < connections.size() &&
                    currentTripId.equals(connections.get(i + 1).getTripId())) {
                i++;
                group.add(connections.get(i));
            }

            // Primera parada: salida del primer tramo
            Stop departure = stopRepository.findById(
                    group.get(0).getDepartureStopId()).orElse(null);
            // Última parada: llegada del último tramo
            Stop arrival = stopRepository.findById(
                    group.get(group.size() - 1).getArrivalStopId()).orElse(null);

            // Paradas intermedias: llegada de cada tramo excepto el último
            List<IntermediateStop> intermediateStops = new ArrayList<>();
            for (int j = 0; j < group.size() - 1; j++) {
                Connection conn = group.get(j);
                Stop intermediateStop = stopRepository
                        .findById(conn.getArrivalStopId()).orElse(null);
                if (intermediateStop != null) {
                    intermediateStops.add(IntermediateStop.builder()
                            .stopId(intermediateStop.getStopId())
                            .stopName(intermediateStop.getStopName())
                            .lat(intermediateStop.getStopLat())
                            .lon(intermediateStop.getStopLon())
                            .arrivalTime(conn.getArrivalTime())
                            .departureTime(group.get(j + 1).getDepartureTime())
                            .build());
                }
            }

            String[] routeData = getRouteData(currentTripId);

            result.add(Connection.builder()
                    .tripId(currentTripId)
                    .departureStopId(group.get(0).getDepartureStopId())
                    .arrivalStopId(group.get(group.size() - 1).getArrivalStopId())
                    .departureTime(group.get(0).getDepartureTime())
                    .arrivalTime(group.get(group.size() - 1).getArrivalTime())
                    .departureSequence(group.get(0).getDepartureSequence())
                    .arrivalSequence(group.get(group.size() - 1).getArrivalSequence())
                    .departureStop(departure)
                    .arrivalStop(arrival)
                    .walkingMinutes(null)
                    .routeShortName(routeData[0])
                    .routeColor(routeData[1])
                    .intermediateStops(intermediateStops)
                    .realtimeDelayMinutes(group.get(0).getRealtimeDelayMinutes())
                    .cancelled(group.get(0).getCancelled())
                    .build());
            i++;
        }

        return result;
    }

    private String[] getRouteData(String tripId) {
        if ("WALK".equals(tripId)) return new String[]{null, null};
        var result = tripRepository.findById(tripId)
                .flatMap(trip -> routeRepository.findById(trip.getRouteId()))
                .map(route -> new String[]{
                        route.getRouteShortName(),
                        route.getRouteColor()
                })
                .orElse(new String[]{null, null});
        log.info("getRouteData for {}: [{}, {}]", tripId, result[0], result[1]);
        return result;
    }
}
