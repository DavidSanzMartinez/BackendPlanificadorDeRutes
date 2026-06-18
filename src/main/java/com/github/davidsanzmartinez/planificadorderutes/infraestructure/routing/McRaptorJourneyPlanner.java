package com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Footpath;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.JourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime.RealtimeDelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class McRaptorJourneyPlanner implements JourneyPlanner {

    private final RoutingDataService routingData;

    private static final int MAX_ROUNDS = 6;
    private static final int MAX_LABELS = 5;

    private final RealtimeDelayService realtimeDelayService;

    @Override
    public List<Journey> findJourneys(String originStopId, String destinationStopId,
                                      LocalDate date, String departureTime) {
        int depMin = RoutingDataService.toMinutes(departureTime);
        Set<String> activeTrips = routingData.getActiveTripsForDate(date);

        // Precarga delays RT una sola vez
        Map<String, Integer> delayByTrip = realtimeDelayService.getDelaysForTrips(activeTrips);

        return mcRaptor(originStopId, destinationStopId, depMin,
                activeTrips, date, delayByTrip);
    }

    private List<Journey> mcRaptor(String origen, String destino, int depMin,
                                   Set<String> activeTrips, LocalDate date,
                                   Map<String, Integer> delayByTrip) {

        Map<String, List<String>> stopsOfRoute = routingData.getStopsOfRoute();
        Map<String, Set<String>> routesAtStop = routingData.getRoutesAtStop();
        Map<String, Map<String, Integer>> stopPositionInRoute =
                routingData.getStopPositionInRoute();
        Map<String, Map<String, Integer>> depTime = routingData.getDepTime();
        Map<String, Map<String, Integer>> arrTime = routingData.getArrTime();
        Map<String, Map<String, List<Object[]>>> departuresIndex =
                routingData.getDeparturesIndexFull();
        List<Footpath> footpathsOrigen = routingData.getFootpaths(origen);

        // tau[k][stop] = lista de etiquetas (arrMin, fiabilidad, parent)
        Map<Integer, Map<String, List<Label>>> tau = new HashMap<>();
        for (int i = 0; i <= MAX_ROUNDS; i++) {
            tau.put(i, new HashMap<>());
        }

        // tau_ast[stop] = mejor tiempo de llegada conocido
        Map<String, Integer> tauAst = new HashMap<>();
        tauAst.put(origen, depMin);
        tau.get(0).put(origen, new ArrayList<>(List.of(
                new Label(depMin, 1.0, null))));

        Set<String> marked = new HashSet<>();
        marked.add(origen);

        // Footpaths desde origen
        for (Footpath fp : footpathsOrigen) {
            int walkArr = depMin + fp.getTransferMinutes();
            if (walkArr < tauAst.getOrDefault(fp.getToStopId(), Integer.MAX_VALUE)) {
                tauAst.put(fp.getToStopId(), walkArr);
                tau.get(0).put(fp.getToStopId(),
                        new ArrayList<>(List.of(new Label(walkArr, 1.0, null))));
            }
        }

        for (int k = 1; k <= MAX_ROUNDS; k++) {
            // Construye Q: route → earliest marked stop
            Map<String, String> Q = new HashMap<>();
            for (String stop : marked) {
                for (String route : routesAtStop.getOrDefault(stop, Set.of())) {
                    if (!Q.containsKey(route)) {
                        Q.put(route, stop);
                    } else {
                        Map<String, Integer> posMap =
                                stopPositionInRoute.getOrDefault(route, Map.of());
                        int posStop = posMap.getOrDefault(stop, Integer.MAX_VALUE);
                        int posQ = posMap.getOrDefault(Q.get(route), Integer.MAX_VALUE);
                        if (posStop < posQ) {
                            Q.put(route, stop);
                        }
                    }
                }
            }

            marked = new HashSet<>();

            for (Map.Entry<String, String> entry : Q.entrySet()) {
                String route = entry.getKey();
                String boardStop = entry.getValue();

                List<String> stops = stopsOfRoute.getOrDefault(route, List.of());
                Integer startIdx = stopPositionInRoute
                        .getOrDefault(route, Map.of()).get(boardStop);
                if (startIdx == null) continue;

                List<Label> etiquetasPadre =
                        tau.get(k - 1).getOrDefault(boardStop, List.of());

                for (Label etiquetaPadre : etiquetasPadre) {
                    // Variables locales que se reinician en cada iteración
                    String currentBoardStop = boardStop;
                    Label currentEtiquetaPadre = etiquetaPadre;
                    int arrBoard = etiquetaPadre.arrMin;
                    double fibBoard = etiquetaPadre.fiabilidad;

                    String tNuevo = earliestTrip(route, currentBoardStop, arrBoard,
                            departuresIndex, activeTrips, delayByTrip);
                    if (tNuevo == null) continue;

                    double pBoarding = routingData.probCatch(tNuevo,
                            depTime.getOrDefault(tNuevo, Map.of())
                                    .getOrDefault(currentBoardStop, Integer.MAX_VALUE),
                            arrBoard);
                    double fibNuevaBase = fibBoard * pBoarding;

                    for (int si = startIdx; si < stops.size(); si++) {
                        String stop = stops.get(si);
                        Integer arr = arrTime.getOrDefault(tNuevo, Map.of()).get(stop);

                        if (arr != null &&
                                arr < tauAst.getOrDefault(destino, Integer.MAX_VALUE)) {
                            ParentInfo parent = new ParentInfo(
                                    tNuevo, currentBoardStop, stop, currentEtiquetaPadre, k - 1);
                            Label nueva = new Label(arr, fibNuevaBase, parent);

                            List<Label> bag = tau.get(k)
                                    .computeIfAbsent(stop, x -> new ArrayList<>());
                            if (addIfNotDominated(bag, nueva)) {
                                tauAst.merge(stop, arr, Math::min);
                                marked.add(stop);
                            }
                        }

                        // Intento de cambio de trip
                        List<Label> etiquetasStop =
                                tau.get(k - 1).getOrDefault(stop, List.of());
                        if (!etiquetasStop.isEmpty()) {
                            int mejorArrPrev = etiquetasStop.stream()
                                    .mapToInt(l -> l.arrMin).min().orElse(Integer.MAX_VALUE);
                            if (mejorArrPrev < arrBoard) {
                                for (Label etiquetaAlt : etiquetasStop) {
                                    String tAlt = earliestTrip(route, stop,
                                            etiquetaAlt.arrMin, departuresIndex,
                                            activeTrips, delayByTrip);
                                    if (tAlt != null &&
                                            depTime.getOrDefault(tAlt, Map.of())
                                                    .getOrDefault(stop, Integer.MAX_VALUE)
                                                    <= depTime.getOrDefault(tNuevo, Map.of())
                                                    .getOrDefault(stop, Integer.MAX_VALUE)) {
                                        double pAlt = routingData.probCatch(tAlt,
                                                depTime.getOrDefault(tAlt, Map.of())
                                                        .getOrDefault(stop, Integer.MAX_VALUE),
                                                etiquetaAlt.arrMin);
                                        tNuevo = tAlt;
                                        currentEtiquetaPadre = etiquetaAlt;
                                        arrBoard = etiquetaAlt.arrMin;
                                        fibBoard = etiquetaAlt.fiabilidad;
                                        fibNuevaBase = fibBoard * pAlt;
                                        currentBoardStop = stop;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footpaths
            for (String stop : new HashSet<>(marked)) {
                for (Footpath fp : routingData.getFootpaths(stop)) {
                    List<Label> etiquetasStop =
                            tau.get(k).getOrDefault(stop, List.of());
                    for (Label etiqueta : etiquetasStop) {
                        int walkArr = etiqueta.arrMin + fp.getTransferMinutes();
                        if (walkArr < tauAst.getOrDefault(
                                destino, Integer.MAX_VALUE)) {
                            ParentInfo parent = new ParentInfo(
                                    "WALK", stop, fp.getToStopId(), etiqueta, k);
                            Label nueva = new Label(walkArr, etiqueta.fiabilidad,
                                    parent);
                            List<Label> bag = tau.get(k)
                                    .computeIfAbsent(fp.getToStopId(),
                                            x -> new ArrayList<>());
                            if (addIfNotDominated(bag, nueva)) {
                                tauAst.merge(fp.getToStopId(), walkArr, Math::min);
                                marked.add(fp.getToStopId());
                            }
                        }
                    }
                }
            }

            if (marked.isEmpty()) break;
        }

        return reconstructJourneys(tau, destino, origen, depTime, arrTime);
    }

    private List<Journey> reconstructJourneys(
            Map<Integer, Map<String, List<Label>>> tau,
            String destino,
            String origen,
            Map<String, Map<String, Integer>> depTime,
            Map<String, Map<String, Integer>> arrTime) {

        List<Journey> journeys = new ArrayList<>();

        for (int k = 1; k <= MAX_ROUNDS; k++) {
            List<Label> etiquetasDestino =
                    tau.get(k).getOrDefault(destino, List.of());
            for (Label etiquetaDestino : etiquetasDestino) {
                if (etiquetaDestino.parent == null) continue;

                List<Connection> path = new ArrayList<>();
                Label etiquetaActual = etiquetaDestino;

                while (true) {
                    ParentInfo parent = etiquetaActual.parent;
                    if (parent == null) break;

                    String trip = parent.trip;
                    String boardStop = parent.boardStop;
                    String arrStop = parent.arrStop;
                    Label etiquetaPadre = parent.etiquetaPadre;

                    if ("WALK".equals(trip)) {
                        int arrWalk = etiquetaActual.arrMin;
                        int arrPrev = etiquetaPadre.arrMin;
                        path.add(Connection.builder()
                                .tripId("WALK")
                                .departureStopId(boardStop)
                                .arrivalStopId(arrStop)
                                .departureTime(RoutingDataService.minutesToHhmm(arrPrev))
                                .arrivalTime(RoutingDataService.minutesToHhmm(arrWalk))
                                .departureSequence(0)
                                .arrivalSequence(0)
                                .realtimeDelayMinutes(null)
                                .cancelled(null)
                                .build());
                    } else {
                        Integer depMin = depTime.getOrDefault(trip, Map.of())
                                .get(boardStop);
                        Integer arrMin = arrTime.getOrDefault(trip, Map.of())
                                .get(arrStop);
                        if (depMin == null || arrMin == null) {
                            etiquetaActual = etiquetaPadre;
                            continue;
                        }
                        RealtimeDelayService.RealtimeInfo rt = realtimeDelayService.getRealtimeInfo(trip);
                        path.add(Connection.builder()
                                .tripId(trip)
                                .departureStopId(boardStop)
                                .arrivalStopId(arrStop)
                                .departureTime(RoutingDataService.minutesToHhmm(depMin))
                                .arrivalTime(RoutingDataService.minutesToHhmm(arrMin))
                                .departureSequence(0)
                                .arrivalSequence(0)
                                .realtimeDelayMinutes(rt.delayMinutes())
                                .cancelled(rt.cancelled())
                                .build());
                    }
                    etiquetaActual = etiquetaPadre;
                }

                Collections.reverse(path);

                path = path.stream()
                        .filter(c -> !c.getDepartureStopId().equals(c.getArrivalStopId()))
                        .filter(c -> {
                            int dep = RoutingDataService.toMinutes(c.getDepartureTime());
                            int arr = RoutingDataService.toMinutes(c.getArrivalTime());
                            return arr >= dep;
                        })
                        .collect(Collectors.toCollection(ArrayList::new));


                if (path.isEmpty()) continue;

                if (!path.get(0).getDepartureStopId().equals(origen)) continue;

                boolean transbordoImposible = false;
                for (int i = 1; i < path.size(); i++) {
                    Connection prev = path.get(i - 1);
                    Connection curr = path.get(i);
                    if ("WALK".equals(curr.getTripId())) continue;
                    int arrPrev = RoutingDataService.toMinutes(prev.getArrivalTime());
                    int delayMinutes = realtimeDelayService.getDelaySeconds(curr.getTripId()) / 60;
                    int depCurrReal = RoutingDataService.toMinutes(curr.getDepartureTime()) + delayMinutes;
                    if (arrPrev > depCurrReal) {
                        transbordoImposible = true;
                        break;
                    }
                }
                if (transbordoImposible) continue;

                int transbordos = 0;
                for (int i = 1; i < path.size(); i++) {
                    if (!path.get(i).getTripId().equals(path.get(i - 1).getTripId())
                            && !"WALK".equals(path.get(i).getTripId())) {
                        transbordos++;
                    }
                }

                boolean hasRealtimeData = path.stream()
                        .anyMatch(c -> c.getRealtimeDelayMinutes() != null || c.getCancelled() != null);

                double fiabilidad = 1.0;
                for (int i = 1; i < path.size(); i++) {
                    Connection prev = path.get(i - 1);
                    Connection curr = path.get(i);
                    if ("WALK".equals(curr.getTripId())) continue;
                    int arrPrev = RoutingDataService.toMinutes(prev.getArrivalTime());
                    int depCurr = RoutingDataService.toMinutes(curr.getDepartureTime());
                    fiabilidad *= routingData.probCatch(curr.getTripId(), depCurr, arrPrev);
                }

                // Último trip no-WALK del path
                String lastTripId = null;
                for (int i = path.size() - 1; i >= 0; i--) {
                    if (!"WALK".equals(path.get(i).getTripId())) {
                        lastTripId = path.get(i).getTripId();
                        break;
                    }
                }
                double expectedDelay = lastTripId != null ?
                        routingData.getRetrasoMedio(lastTripId) : 0.0;

                // transferReliability solo si hay transbordos
                Double transferReliability = transbordos > 0 ? fiabilidad : null;


                journeys.add(Journey.builder()
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
                        .transferReliability(transferReliability)
                        .expectedDelayMinutes(expectedDelay)
                        .hasRealtimeData(hasRealtimeData)
                        .build());
            }
        }

        // Deduplica journeys con el mismo path
        Set<String> vistas = new HashSet<>();
        List<Journey> unicos = new ArrayList<>();
        for (Journey j : journeys) {
            String clave = j.getConnections().stream()
                    .map(c -> c.getTripId() + c.getDepartureStopId() + c.getArrivalStopId())
                    .collect(Collectors.joining("|"));
            if (vistas.add(clave)) {
                unicos.add(j);
            }
        }
        return unicos;
    }

    private String earliestTrip(String route, String stop, int minDep,
                                Map<String, Map<String, List<Object[]>>> departuresIndex,
                                Set<String> activeTrips,
                                Map<String, Integer> delayByTrip) {
        List<Object[]> deps = departuresIndex
                .getOrDefault(route, Map.of())
                .getOrDefault(stop, List.of());
        for (Object[] entry : deps) {
            int time = (int) entry[0];
            String tripId = (String) entry[1];
            int delayMinutes = delayByTrip.getOrDefault(tripId, 0);
            int realDep = time + delayMinutes;
            if (realDep >= minDep && activeTrips.contains(tripId)) {
                return tripId;
            }
        }
        return null;
    }

    private boolean addIfNotDominated(List<Label> bag, Label nueva) {
        if (bag.stream().anyMatch(e -> dominates(e, nueva))) return false;
        bag.removeIf(e -> dominates(nueva, e));
        bag.add(nueva);
        if (bag.size() > MAX_LABELS) {
            bag.sort(Comparator.comparingInt(l -> l.arrMin));
            bag.subList(MAX_LABELS, bag.size()).clear();
        }
        return true;
    }

    private boolean dominates(Label a, Label b) {
        return a.arrMin <= b.arrMin && a.fiabilidad >= b.fiabilidad
                && (a.arrMin < b.arrMin || a.fiabilidad > b.fiabilidad);
    }

    private String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h > 0 ? h + "h " + m + "min" : m + "min";
    }

    // ── Clases auxiliares ──
    private static class Label {
        int arrMin;
        double fiabilidad;
        ParentInfo parent;

        Label(int arrMin, double fiabilidad, ParentInfo parent) {
            this.arrMin = arrMin;
            this.fiabilidad = fiabilidad;
            this.parent = parent;
        }
    }

    private static class ParentInfo {
        String trip;
        String boardStop;
        String arrStop;
        Label etiquetaPadre;
        int kPadre;

        ParentInfo(String trip, String boardStop, String arrStop,
                   Label etiquetaPadre, int kPadre) {
            this.trip = trip;
            this.boardStop = boardStop;
            this.arrStop = arrStop;
            this.etiquetaPadre = etiquetaPadre;
            this.kPadre = kPadre;
        }
    }
}