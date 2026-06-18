package com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Footpath;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.JourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.FootpathEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.FootpathJpaRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime.RealtimeDelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsaMeatJourneyPlanner implements JourneyPlanner {

    private final RoutingDataService routingData;

    private final RealtimeDelayService realtimeDelayService;

    @Override
    public List<Journey> findJourneys(String originStopId, String destinationStopId,
                                      LocalDate date, String departureTime) {

        int depMin = RoutingDataService.toMinutes(departureTime);

        List<Connection> activeConnections = routingData.getConnectionsForDateRange(date, depMin);

        return csaMeat(activeConnections, originStopId, destinationStopId, depMin);
    }

    private List<Journey> csaMeat(List<Connection> connections,
                                  String origen, String destino, int depMin) {
        Map<String, Double> S = new HashMap<>();
        Map<String, Integer> sDet = new HashMap<>();
        Map<String, Integer> T = new HashMap<>();
        Map<String, Connection> J = new HashMap<>();

        S.put(origen, (double) depMin);
        sDet.put(origen, depMin);

        // Footpaths desde origen
        for (Footpath fp : routingData.getFootpaths(origen)) {
            int walkArr = depMin + fp.getTransferMinutes();
            S.put(fp.getToStopId(), (double) walkArr);
            sDet.put(fp.getToStopId(), walkArr);
            J.put(fp.getToStopId(), Connection.builder()
                    .tripId("WALK")
                    .departureStopId(origen)
                    .arrivalStopId(fp.getToStopId())
                    .departureTime(RoutingDataService.minutesToHhmm(depMin))
                    .arrivalTime(RoutingDataService.minutesToHhmm(walkArr))
                    .departureSequence(0)
                    .arrivalSequence(0)
                    .realtimeDelayMinutes(null)
                    .cancelled(null)
                    .build());
        }

        for (Connection c : connections) {
            int depStop_det = sDet.getOrDefault(c.getDepartureStopId(), Integer.MAX_VALUE);
            int dest_det = sDet.getOrDefault(destino, Integer.MAX_VALUE);
            int cDepMin = RoutingDataService.toMinutes(c.getDepartureTime());
            int cArrMin = RoutingDataService.toMinutes(c.getArrivalTime());

            if (cDepMin > dest_det) break;

            int tTrip = T.getOrDefault(c.getTripId(), Integer.MAX_VALUE);

            if (tTrip <= cDepMin || depStop_det <= cDepMin) {
                T.merge(c.getTripId(), depStop_det, Math::min);

                if (c.getArrivalStopId().equals(origen)) continue;

                double p = routingData.probCatchWithRT(c.getTripId(), cDepMin, depStop_det);

                if (cArrMin < sDet.getOrDefault(c.getArrivalStopId(), Integer.MAX_VALUE)) {
                    sDet.put(c.getArrivalStopId(), cArrMin);
                }

                if (p > 0) {
                    double retrasoMedio = routingData.getRetrasoMedio(c.getTripId());
                    double expectedArrEnTren = cArrMin + retrasoMedio;
                    double sArrStop = S.getOrDefault(c.getArrivalStopId(),
                            Double.MAX_VALUE);

                    double expectedArr;
                    if (p >= 1.0 || sArrStop == Double.MAX_VALUE) {
                        expectedArr = expectedArrEnTren;
                    } else {
                        expectedArr = p * expectedArrEnTren + (1 - p) * sArrStop;
                    }

                    if (expectedArr < sArrStop) {
                        S.put(c.getArrivalStopId(), expectedArr);
                        RealtimeDelayService.RealtimeInfo rt = realtimeDelayService.getRealtimeInfo(c.getTripId());
                        J.put(c.getArrivalStopId(), c.toBuilder()
                                .realtimeDelayMinutes(rt.delayMinutes())
                                .cancelled(rt.cancelled())
                                .build());

                        for (Footpath fp : routingData.getFootpaths(c.getArrivalStopId())) {
                            int walkArr = cArrMin + fp.getTransferMinutes();
                            if (walkArr < sDet.getOrDefault(fp.getToStopId(),
                                    Integer.MAX_VALUE)) {
                                sDet.put(fp.getToStopId(), walkArr);
                            }
                            double sWalk = S.getOrDefault(fp.getToStopId(),
                                    Double.MAX_VALUE);
                            if (walkArr < sWalk) {
                                S.put(fp.getToStopId(), (double) walkArr);
                                J.put(fp.getToStopId(), Connection.builder()
                                        .tripId("WALK")
                                        .departureStopId(c.getArrivalStopId())
                                        .arrivalStopId(fp.getToStopId())
                                        .departureTime(RoutingDataService
                                                .minutesToHhmm(cArrMin))
                                        .arrivalTime(RoutingDataService
                                                .minutesToHhmm(walkArr))
                                        .departureSequence(0)
                                        .arrivalSequence(0)
                                        .realtimeDelayMinutes(null)
                                        .cancelled(null)
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        if (!sDet.containsKey(destino)) return List.of();

        // Reconstruye el camino
        List<Connection> path = new ArrayList<>();
        String stopActual = destino;
        Set<String> visited = new HashSet<>();

        while (J.containsKey(stopActual) && !visited.contains(stopActual)) {
            visited.add(stopActual);
            Connection leg = J.get(stopActual);
            path.add(leg);
            stopActual = leg.getDepartureStopId();
        }

        Collections.reverse(path);
        if (path.isEmpty()) return List.of();

        int transbordos = 0;
        for (int i = 1; i < path.size(); i++) {
            if (!path.get(i).getTripId().equals(path.get(i - 1).getTripId())
                    && !"WALK".equals(path.get(i).getTripId())) {
                transbordos++;
            }
        }

        boolean hasRealtimeData = path.stream()
                .anyMatch(c -> c.getRealtimeDelayMinutes() != null || c.getCancelled() != null);

        // Fiabilidad acumulada sobre el path reconstruido
        double fiabilidadAcumulada = 1.0;
        for (int i = 1; i < path.size(); i++) {
            Connection prev = path.get(i - 1);
            Connection curr = path.get(i);
            if ("WALK".equals(curr.getTripId())) continue;
            int arrPrev = RoutingDataService.toMinutes(prev.getArrivalTime());
            int depCurr = RoutingDataService.toMinutes(curr.getDepartureTime());
            fiabilidadAcumulada *= routingData.probCatchWithRT(curr.getTripId(), depCurr, arrPrev);
        }
        Double transferReliability = transbordos > 0 ? fiabilidadAcumulada : null;

        // Retraso esperado del último trip no-WALK
        String lastTripId = null;
        for (int i = path.size() - 1; i >= 0; i--) {
            if (!"WALK".equals(path.get(i).getTripId())) {
                lastTripId = path.get(i).getTripId();
                break;
            }
        }
        double expectedDelay = lastTripId != null ? routingData.getRetrasoMedio(lastTripId) : 0.0;

        return List.of(Journey.builder()
                .origin(null)
                .destination(null)
                .departureTime(path.get(0).getDepartureTime())
                .arrivalTime(path.get(path.size() - 1).getArrivalTime())
                .totalDuration(formatDuration(
                        RoutingDataService.toMinutes(
                                path.get(path.size() - 1).getArrivalTime())
                                - RoutingDataService.toMinutes(
                                path.get(0).getDepartureTime())))
                .numTransfers(transbordos)
                .connections(path)
                .hasRealtimeData(hasRealtimeData)
                .transferReliability(transferReliability)
                .expectedDelayMinutes(expectedDelay)
                .build());
    }

    private String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h > 0 ? h + "h " + m + "min" : m + "min";
    }
}