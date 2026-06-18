package com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Footpath;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.CalendarEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopTimeEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.*;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime.RealtimeDelayService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class RoutingDataService {

    private final StopTimeJpaRepository stopTimeRepository;
    private final TripJpaRepository tripRepository;
    private final RouteJpaRepository routeRepository;
    private final CalendarJpaRepository calendarRepository;
    private final CalendarDateJpaRepository calendarDateRepository;
    private final FootpathJpaRepository footpathRepository;
    private final StopJpaRepository stopRepository;

    // ── Conexiones ordenadas por hora de salida ──
    private List<Connection> connections = new ArrayList<>();

    // ── Índices de calendario ──
    private Map<String, CalendarEntity> calendarByServiceId = new HashMap<>();
    private Map<String, Map<LocalDate, Integer>> calendarDateExceptions = new HashMap<>();
    private Map<String, String> serviceIdByTripId = new HashMap<>();

    // ── Índice de footpaths ──
    private Map<String, List<Footpath>> footpathsByStop = new HashMap<>();

    // ── Índice de fiabilidad: trip_id → tipo de servicio ──
    private Map<String, String> tripToTipo = new HashMap<>();

    // ── Parámetros de distribución log-normal por tipo ──
    private Map<String, double[]> reliabilityParams = new HashMap<>();

    // ── Estructuras RAPTOR ──
    private Map<String, List<String>> stopsOfRoute = new HashMap<>();
    private Map<String, Set<String>> routesAtStop = new HashMap<>();
    private Map<String, Map<String, Integer>> stopPositionInRoute = new HashMap<>();
    private Map<String, Map<String, Integer>> depTime = new HashMap<>();
    private Map<String, Map<String, Integer>> arrTime = new HashMap<>();
    private Map<String, List<String>> tripsOfRoute = new HashMap<>();
    private Map<String, String> tripToRoute = new HashMap<>();
    private Map<String, Map<String, List<int[]>>> departuresIndex = new HashMap<>();

    private Map<String, Map<String, List<Object[]>>> departuresIndexFull = new HashMap<>();

    private final RealtimeDelayService realtimeDelayService;

    // ── Centroides aproximados de núcleos de cercanías (lat, lon) ──
    private static final Map<String, double[]> CENTROIDES_NUCLEOS = Map.ofEntries(
            Map.entry("MADRID",         new double[]{40.40, -3.70}),
            Map.entry("BARCELONA",      new double[]{41.40,  2.10}),
            Map.entry("SEVILLA",        new double[]{37.38, -5.98}),
            Map.entry("VALENCIA",       new double[]{39.47, -0.37}),
            Map.entry("BILBAO",         new double[]{43.26, -2.93}),
            Map.entry("ASTURIAS",       new double[]{43.36, -5.85}),
            Map.entry("MALAGA",         new double[]{36.72, -4.42}),
            Map.entry("MURCIAALICANTE", new double[]{38.10, -1.20}),
            Map.entry("SANTANDER",      new double[]{43.46, -3.81}),
            Map.entry("SANSEBASTIAN",   new double[]{43.32, -1.97}),
            Map.entry("ZARAGOZA",       new double[]{41.66, -0.88})
    );

    @EventListener(ApplicationReadyEvent.class)
    public void buildIndex() {
        log.info("Building routing data index...");
        buildCalendarIndex();
        buildConnections();
        buildFootpathsIndex();
        buildReliabilityIndex(); //Siempre antes que buildConnectiones
        log.info("Routing data index built — {} connections, {} footpath stops",
                connections.size(), footpathsByStop.size());
        buildRaptorStructures();
    }

    private void buildRaptorStructures() {
        log.info("Building RAPTOR structures...");

        // trip_id → route_id
        tripToRoute = tripRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getTripId().trim(),
                        t -> t.getRouteId().trim(),
                        (a, b) -> a
                ));

        // dep_time y arr_time por trip y parada
        // stops_of_route y stop_position_in_route
        Map<String, Map<Integer, String>> routeSeqStop = new HashMap<>();

        for (Connection conn : connections) {
            String tripId = conn.getTripId();
            String routeId = tripToRoute.get(tripId);
            if (routeId == null) continue;

            depTime.computeIfAbsent(tripId, k -> new HashMap<>())
                    .put(conn.getDepartureStopId(),
                            toMinutes(conn.getDepartureTime()));
            arrTime.computeIfAbsent(tripId, k -> new HashMap<>())
                    .put(conn.getArrivalStopId(),
                            toMinutes(conn.getArrivalTime()));

            routeSeqStop.computeIfAbsent(routeId, k -> new HashMap<>())
                    .put(conn.getDepartureSequence(), conn.getDepartureStopId());
            routeSeqStop.computeIfAbsent(routeId, k -> new HashMap<>())
                    .put(conn.getArrivalSequence(), conn.getArrivalStopId());
        }

        // stops_of_route ordenadas
        for (Map.Entry<String, Map<Integer, String>> e : routeSeqStop.entrySet()) {
            List<String> stops = e.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .toList();
            stopsOfRoute.put(e.getKey(), stops);
        }

        // stop_position_in_route
        for (Map.Entry<String, List<String>> e : stopsOfRoute.entrySet()) {
            Map<String, Integer> posMap = new HashMap<>();
            List<String> stops = e.getValue();
            for (int i = 0; i < stops.size(); i++) {
                posMap.put(stops.get(i), i);
            }
            stopPositionInRoute.put(e.getKey(), posMap);
        }

        // routes_at_stop
        for (Map.Entry<String, List<String>> e : stopsOfRoute.entrySet()) {
            for (String stop : e.getValue()) {
                routesAtStop.computeIfAbsent(stop, k -> new HashSet<>())
                        .add(e.getKey());
            }
        }

        // trips_of_route ordenados por hora de salida en primera parada
        Map<String, List<int[]>> routeTripDeps = new HashMap<>();
        for (Map.Entry<String, String> e : tripToRoute.entrySet()) {
            String tripId = e.getKey();
            String routeId = e.getValue();
            List<String> stops = stopsOfRoute.get(routeId);
            if (stops == null || stops.isEmpty()) continue;
            String firstStop = stops.get(0);
            Integer dep = depTime.getOrDefault(tripId, Map.of()).get(firstStop);
            if (dep == null) dep = Integer.MAX_VALUE;
            routeTripDeps.computeIfAbsent(routeId, k -> new ArrayList<>())
                    .add(new int[]{dep, tripId.hashCode()});
        }

        // trips_of_route como listas ordenadas
        Map<String, List<String>> routeTripsSorted = new HashMap<>();
        for (Map.Entry<String, List<int[]>> e : routeTripDeps.entrySet()) {
            // Necesitamos mantener el tripId, no solo el hash
        }

        // Versión correcta con par (dep, tripId)
        Map<String, List<Map.Entry<Integer, String>>> routeTripsWithDep = new HashMap<>();
        for (Map.Entry<String, String> e : tripToRoute.entrySet()) {
            String tripId = e.getKey();
            String routeId = e.getValue();
            List<String> stops = stopsOfRoute.get(routeId);
            if (stops == null || stops.isEmpty()) continue;
            String firstStop = stops.get(0);
            Integer dep = depTime.getOrDefault(tripId, Map.of()).get(firstStop);
            if (dep == null) dep = Integer.MAX_VALUE;
            routeTripsWithDep.computeIfAbsent(routeId, k -> new ArrayList<>())
                    .add(Map.entry(dep, tripId));
        }

        tripsOfRoute = new HashMap<>();
        for (Map.Entry<String, List<Map.Entry<Integer, String>>> e :
                routeTripsWithDep.entrySet()) {
            tripsOfRoute.put(e.getKey(),
                    e.getValue().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(Map.Entry::getValue)
                            .toList());
        }

        // departures_index: route → stop → [(time, tripId)] ordenado
        // departures_index correcto: route → stop → [(time, tripId)] ordenado
        departuresIndexFull = new HashMap<>();
        for (Map.Entry<String, List<String>> e : tripsOfRoute.entrySet()) {
            String routeId = e.getKey();
            for (String tripId : e.getValue()) {
                Map<String, Integer> deps = depTime.getOrDefault(tripId, Map.of());
                for (Map.Entry<String, Integer> d : deps.entrySet()) {
                    departuresIndexFull
                            .computeIfAbsent(routeId, k -> new HashMap<>())
                            .computeIfAbsent(d.getKey(), k -> new ArrayList<>())
                            .add(new Object[]{d.getValue(), tripId});
                }
            }
        }
        // Ordena cada lista por tiempo
        for (Map<String, List<Object[]>> routeMap : departuresIndexFull.values()) {
            for (List<Object[]> list : routeMap.values()) {
                list.sort(Comparator.comparingInt(a -> (int) a[0]));
            }
        }

        log.info("RAPTOR structures built — {} routes, {} stops with routes",
                stopsOfRoute.size(), routesAtStop.size());
    }

    private void buildCalendarIndex() {
        calendarByServiceId = calendarRepository.findAll().stream()
                .collect(Collectors.toMap(
                        CalendarEntity::getServiceId, c -> c));

        calendarDateExceptions = new HashMap<>();
        calendarDateRepository.findAll().forEach(cd -> {
            calendarDateExceptions
                    .computeIfAbsent(cd.getId().getServiceId(), k -> new HashMap<>())
                    .put(cd.getId().getDate(), cd.getExceptionType());
        });

        serviceIdByTripId = tripRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getTripId().trim(),
                        t -> t.getServiceId().trim()
                ));
    }

    private void buildConnections() {
        List<StopTimeEntity> allStopTimes =
                stopTimeRepository.findAllOrderedByDepartureTime();

        Map<String, List<StopTimeEntity>> byTrip = allStopTimes.stream()
                .collect(Collectors.groupingBy(
                        st -> st.getId().getTripId()));

        List<Connection> result = new ArrayList<>();

        for (Map.Entry<String, List<StopTimeEntity>> entry : byTrip.entrySet()) {
            String tripId = entry.getKey();
            List<StopTimeEntity> stopTimes = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(
                            st -> st.getId().getStopSequence()))
                    .toList();

            for (int i = 0; i < stopTimes.size() - 1; i++) {
                StopTimeEntity dep = stopTimes.get(i);
                StopTimeEntity arr = stopTimes.get(i + 1);

                if (dep.getPickupType() != null && dep.getPickupType() == 1) continue;
                if (arr.getDropOffType() != null && arr.getDropOffType() == 1) continue;

                result.add(Connection.builder()
                        .tripId(tripId)
                        .departureStopId(dep.getStopId())
                        .arrivalStopId(arr.getStopId())
                        .departureTime(dep.getDepartureTime())
                        .arrivalTime(arr.getArrivalTime())
                        .departureSequence(dep.getId().getStopSequence())
                        .arrivalSequence(arr.getId().getStopSequence())
                        .build());
            }
        }

        result.sort(Comparator.comparingInt(c -> toMinutes(c.getDepartureTime())));
        connections = result;
        log.info("Built {} connections", connections.size());
    }

    private void buildFootpathsIndex() {
        footpathsByStop = new HashMap<>();
        footpathRepository.findAll().forEach(fp ->
                footpathsByStop
                        .computeIfAbsent(fp.getId().getFromStopId(),
                                k -> new ArrayList<>())
                        .add(Footpath.builder()
                                .toStopId(fp.getId().getToStopId())
                                .transferMinutes(fp.getTransferTime() / 60)
                                .build())
        );
    }

    private void buildReliabilityIndex() {
        // Parámetros calibrados con datos reales de Renfe
        reliabilityParams = new HashMap<>();

        // ── Larga distancia ──
        reliabilityParams.put("AVE",      new double[]{1.690, 1.112, 8.92});
        reliabilityParams.put("ALVIA",    new double[]{1.751, 1.348, 13.50});
        reliabilityParams.put("MD",       new double[]{1.632, 1.115, 9.12});
        reliabilityParams.put("AVANT",    new double[]{0.401, 1.632, 5.68});
        reliabilityParams.put("REGIONAL", new double[]{1.617, 0.988, 7.67});
        reliabilityParams.put("EUROMED",  new double[]{1.700, 1.146, 9.43});
        reliabilityParams.put("AVLO",     new double[]{1.617, 0.982, 7.61});

        // ── Cercanías genéricos (fallback por núcleo) ──
        reliabilityParams.put("Cercanias_Madrid",    new double[]{1.521, 0.606, 5.5});
        reliabilityParams.put("Rodalies_Barcelona",  new double[]{2.019, 0.790, 10.5});
        reliabilityParams.put("Cercanias_Sevilla",   new double[]{1.936, 0.616, 8.0});
        reliabilityParams.put("Cercanias_Bilbao",    new double[]{0.804, 0.672, 2.8});
        reliabilityParams.put("Cercanias_Valencia",  new double[]{1.077, 0.796, 4.0});

        // ── Cercanías por línea (calibrados con retrasosrenfe.com) ──
        reliabilityParams.put("C2_ASTURIAS", new double[]{0.584, 1.509, 5.6});
        reliabilityParams.put("C4_ASTURIAS", new double[]{0.704, 1.443, 5.73});
        reliabilityParams.put("C6_ASTURIAS", new double[]{1.037, 1.315, 6.69});
        reliabilityParams.put("C7_ASTURIAS", new double[]{1.467, 1.092, 7.87});
        reliabilityParams.put("R1_BARCELONA", new double[]{0.966, 1.345, 6.49});
        reliabilityParams.put("R11_BARCELONA", new double[]{2.406, 0.726, 14.44});
        reliabilityParams.put("R14_BARCELONA", new double[]{2.527, 0.667, 15.63});
        reliabilityParams.put("R15_BARCELONA", new double[]{1.607, 1.07, 8.83});
        reliabilityParams.put("R16_BARCELONA", new double[]{1.763, 0.995, 9.57});
        reliabilityParams.put("R17_BARCELONA", new double[]{1.543, 1.097, 8.54});
        reliabilityParams.put("R2_BARCELONA", new double[]{2.102, 0.857, 11.81});
        reliabilityParams.put("R2N_BARCELONA", new double[]{1.828, 0.974, 10.0});
        reliabilityParams.put("R2S_BARCELONA", new double[]{2.242, 0.796, 12.92});
        reliabilityParams.put("R3_BARCELONA", new double[]{1.904, 0.942, 10.46});
        reliabilityParams.put("R4_BARCELONA", new double[]{1.868, 0.957, 10.24});
        reliabilityParams.put("R8_BARCELONA", new double[]{0.587, 1.478, 5.36});
        reliabilityParams.put("RG1_BARCELONA", new double[]{0.737, 0.926, 3.21});
        reliabilityParams.put("C1_BILBAO", new double[]{-1.279, 2.066, 2.35});
        reliabilityParams.put("C2_BILBAO", new double[]{0.019, 1.533, 3.3});
        reliabilityParams.put("C10_MADRID", new double[]{0.962, 1.346, 6.48});
        reliabilityParams.put("C2_MADRID", new double[]{1.05, 1.294, 6.6});
        reliabilityParams.put("C3_MADRID", new double[]{0.527, 1.534, 5.49});
        reliabilityParams.put("C4_MADRID", new double[]{1.142, 1.232, 6.69});
        reliabilityParams.put("C4A_MADRID", new double[]{0.709, 1.455, 5.86});
        reliabilityParams.put("C4B_MADRID", new double[]{0.445, 1.562, 5.28});
        reliabilityParams.put("C5_MADRID", new double[]{0.894, 1.376, 6.3});
        reliabilityParams.put("C7_MADRID", new double[]{1.372, 1.133, 7.49});
        reliabilityParams.put("C8_MADRID", new double[]{1.013, 1.295, 6.37});
        reliabilityParams.put("C1_MALAGA", new double[]{0.397, 1.158, 2.91});
        reliabilityParams.put("C2_MALAGA", new double[]{0.071, 1.485, 3.24});
        reliabilityParams.put("C1_MURCIAALICANTE", new double[]{0.436, 1.573, 5.33});
        reliabilityParams.put("C1_SANSEBASTIAN", new double[]{-0.308, 1.759, 3.45});
        reliabilityParams.put("C1_SANTANDER", new double[]{1.979, 0.872, 10.58});
        reliabilityParams.put("C1_SEVILLA", new double[]{2.268, 0.785, 13.15});
        reliabilityParams.put("C2_SEVILLA", new double[]{0.714, 1.375, 5.25});
        reliabilityParams.put("C4_SEVILLA", new double[]{1.667, 0.869, 7.73});
        reliabilityParams.put("C5_SEVILLA", new double[]{1.504, 1.114, 8.36});
        reliabilityParams.put("C1_VALENCIA", new double[]{-0.069, 1.676, 3.8});
        reliabilityParams.put("C2_VALENCIA", new double[]{1.045, 1.233, 6.08});
        reliabilityParams.put("C6_VALENCIA", new double[]{0.436, 1.573, 5.33});

        // Centroides geográficos de cada núcleo
        Map<String, double[]> centroidesNucleos = Map.ofEntries(
                Map.entry("MADRID",         new double[]{40.40, -3.70}),
                Map.entry("BARCELONA",      new double[]{41.40,  2.10}),
                Map.entry("SEVILLA",        new double[]{37.38, -5.98}),
                Map.entry("VALENCIA",       new double[]{39.47, -0.37}),
                Map.entry("BILBAO",         new double[]{43.26, -2.93}),
                Map.entry("ASTURIAS",       new double[]{43.36, -5.85}),
                Map.entry("MALAGA",         new double[]{36.72, -4.42}),
                Map.entry("MURCIAALICANTE", new double[]{38.10, -1.20}),
                Map.entry("SANTANDER",      new double[]{43.46, -3.81}),
                Map.entry("SANSEBASTIAN",   new double[]{43.32, -1.97}),
                Map.entry("ZARAGOZA",       new double[]{41.66, -0.88})
        );

        Map<String, String> nucleoToGenerico = Map.of(
                "MADRID",    "Cercanias_Madrid",
                "BARCELONA", "Rodalies_Barcelona",
                "SEVILLA",   "Cercanias_Sevilla",
                "BILBAO",    "Cercanias_Bilbao",
                "VALENCIA",  "Cercanias_Valencia"
        );

        // Coordenadas de cada parada
        Map<String, double[]> stopCoords = new HashMap<>();
        stopRepository.findAll().forEach(s -> {
            if (s.getStopLat() != 0 || s.getStopLon() != 0) {
                stopCoords.put(s.getStopId(), new double[]{s.getStopLat(), s.getStopLon()});
            }
        });

        // Stops de cada trip (vía stop_times)
        Map<String, List<String>> stopsByTrip = new HashMap<>();
        stopTimeRepository.findAll().forEach(st ->
                stopsByTrip.computeIfAbsent(st.getId().getTripId(), k -> new ArrayList<>())
                        .add(st.getStopId())
        );

        // Mapeo trip_id → tipo
        tripToTipo = new HashMap<>();
        tripRepository.findAll().forEach(trip -> {
            String tripId = trip.getTripId().trim();
            String routeId = trip.getRouteId();
            String routeShortName = "";
            var routeOpt = routeRepository.findById(routeId);
            if (routeOpt.isPresent()) {
                routeShortName = (routeOpt.get().getRouteShortName() != null ?
                        routeOpt.get().getRouteShortName() : "").toUpperCase();
            }

            // Caso 1: línea de cercanías (C% o R%) — determinar núcleo por geografía
            if (routeShortName.matches("^[CR][A-Z]?\\d+[A-Z]*$")) {
                List<String> stops = stopsByTrip.getOrDefault(tripId, List.of());
                String nucleo = nucleoMasCercano(stops, stopCoords, centroidesNucleos);
                if (nucleo != null) {
                    String claveLinea = routeShortName + "_" + nucleo;
                    if (reliabilityParams.containsKey(claveLinea)) {
                        tripToTipo.put(tripId, claveLinea);
                        return;
                    }
                    String generico = nucleoToGenerico.get(nucleo);
                    if (generico != null) {
                        tripToTipo.put(tripId, generico);
                        return;
                    }
                }
            }

            // Caso 2: LD u otras — mapeo por nombre como antes
            for (String tipo : reliabilityParams.keySet()) {
                if (routeShortName.contains(tipo.toUpperCase())) {
                    tripToTipo.put(tripId, tipo);
                    break;
                }
            }
        });

        log.info("Reliability index built — {}/{} trips with tipo assigned",
                tripToTipo.size(), serviceIdByTripId.size());
    }

    String nucleoMasCercano(List<String> stops, Map<String, double[]> stopCoords,
                                    Map<String, double[]> centroidesNucleos) {
        if (stops.isEmpty()) return null;
        // Centroide del trip
        double sumLat = 0, sumLon = 0;
        int n = 0;
        for (String s : stops) {
            double[] c = stopCoords.get(s);
            if (c != null) {
                sumLat += c[0];
                sumLon += c[1];
                n++;
            }
        }
        if (n == 0) return null;
        double tripLat = sumLat / n;
        double tripLon = sumLon / n;

        // Núcleo más cercano
        String mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (var entry : centroidesNucleos.entrySet()) {
            double[] c = entry.getValue();
            double dist = Math.pow(c[0] - tripLat, 2) + Math.pow(c[1] - tripLon, 2);
            if (dist < mejorDist) {
                mejorDist = dist;
                mejor = entry.getKey();
            }
        }
        return mejor;
    }

    private final Map<LocalDate, List<Connection>> connectionsCache =
            new ConcurrentHashMap<>();

    public List<Connection> getConnectionsForDate(LocalDate date) {
        return connectionsCache.computeIfAbsent(date, d ->
                connections.stream()
                        .filter(c -> operatesOn(c.getTripId(), d))
                        .toList()
        );
    }

    private final Map<LocalDate, Set<String>> activeTripsCache =
            new ConcurrentHashMap<>();

    public Set<String> getActiveTripsForDate(LocalDate date) {
        return activeTripsCache.computeIfAbsent(date, d ->
                connections.stream()
                        .map(Connection::getTripId)
                        .filter(t -> operatesOn(t, d))
                        .collect(Collectors.toSet())
        );
    }

    // ── Métodos de acceso ──

    public boolean operatesOn(String tripId, LocalDate date) {
        String serviceId = serviceIdByTripId.get(tripId);
        if (serviceId == null) return false;

        Map<LocalDate, Integer> exceptions = calendarDateExceptions.get(serviceId);
        if (exceptions != null && exceptions.containsKey(date)) {
            return exceptions.get(date) == 1;
        }

        CalendarEntity calendar = calendarByServiceId.get(serviceId);
        if (calendar == null) return false;
        if (date.isBefore(calendar.getStartDate()) ||
                date.isAfter(calendar.getEndDate())) return false;

        return switch (date.getDayOfWeek()) {
            case MONDAY -> calendar.isMonday();
            case TUESDAY -> calendar.isTuesday();
            case WEDNESDAY -> calendar.isWednesday();
            case THURSDAY -> calendar.isThursday();
            case FRIDAY -> calendar.isFriday();
            case SATURDAY -> calendar.isSaturday();
            case SUNDAY -> calendar.isSunday();
        };
    }
    /*
    public List<Connection> getConnectionsForDate(LocalDate date) {
        return connections.stream()
                .filter(c -> operatesOn(c.getTripId(), date))
                .toList();
    }
    */
    public List<Footpath> getFootpaths(String stopId) {
        return footpathsByStop.getOrDefault(stopId, List.of());
    }

    public double probCatch(String tripId, int depMin, int arrivalAtStop) {
        int slack = depMin - arrivalAtStop;
        if (slack < 0) return 0.0;
        String tipo = tripToTipo.get(tripId);
        if (tipo == null) return 1.0;
        double[] params = reliabilityParams.get(tipo);
        if (params == null) return 1.0;
        double mu = params[0];
        double sigma = params[1];
        double z = (Math.log(Math.max(slack, 0.001)) - mu) / sigma;
        return 1.0 - normalCDF(z);
    }

    private double normalCDF(double z) {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    private double erf(double x) {
        // Aproximación de Horner
        double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));
        double poly = t * (0.254829592 +
                t * (-0.284496736 +
                        t * (1.421413741 +
                                t * (-1.453152027 +
                                        t * 1.061405429))));
        double result = 1.0 - poly * Math.exp(-x * x);
        return x >= 0 ? result : -result;
    }

    public double getRetrasoMedio(String tripId) {
        String tipo = tripToTipo.get(tripId);
        if (tipo == null) return 0.0;
        double[] params = reliabilityParams.get(tipo);
        return params != null ? params[2] : 0.0;
    }

    public static int toMinutes(String time) {
        if (time == null || time.isBlank()) return 0;
        String[] parts = time.split(":");
        try {
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String minutesToHhmm(int minutes) {
        return String.format("%02d:%02d:00", minutes / 60, minutes % 60);
    }

    private final Map<LocalDate, List<Connection>> connectionsWithRTCache =
            new ConcurrentHashMap<>();
    private volatile long lastRTCacheTime = 0;
    private static final long RT_CACHE_TTL_MS = 30_000;

    public List<Connection> getConnectionsForDateWithRT(LocalDate date) {
        long now = System.currentTimeMillis();
        if (now - lastRTCacheTime > RT_CACHE_TTL_MS) {
            connectionsWithRTCache.clear();
            lastRTCacheTime = now;
        }
        return connectionsWithRTCache.computeIfAbsent(date, d -> {
            List<Connection> base = getConnectionsForDate(d);
            // Trips únicos de esta fecha
            Set<String> uniqueTrips = base.stream()
                    .map(Connection::getTripId)
                    .collect(Collectors.toSet());
            // Una sola consulta MGET para todos los delays
            Map<String, Integer> delayByTrip =
                    realtimeDelayService.getDelaysForTrips(uniqueTrips);
            return base.stream()
                    .map(c -> {
                        int delayMinutes = delayByTrip.getOrDefault(c.getTripId(), 0);
                        if (delayMinutes == 0) return c;
                        int newDep = toMinutes(c.getDepartureTime()) + delayMinutes;
                        int newArr = toMinutes(c.getArrivalTime()) + delayMinutes;
                        return c.toBuilder()
                                .departureTime(minutesToHhmm(newDep))
                                .arrivalTime(minutesToHhmm(newArr))
                                .build();
                    })
                    .toList();
        });
    }

    public List<Connection> getConnectionsForDateRange(LocalDate date, int depMin) {
        List<Connection> today = getConnectionsForDateWithRT(date);
        if (depMin < 18 * 60) return today;  // No extender si sale antes de las 18:00

        // Añadir conexiones del día siguiente con tiempos +1440
        List<Connection> tomorrow = getConnectionsForDateWithRT(date.plusDays(1));
        List<Connection> extended = new ArrayList<>(today);
        for (Connection c : tomorrow) {
            int newDep = toMinutes(c.getDepartureTime()) + 1440;
            int newArr = toMinutes(c.getArrivalTime()) + 1440;
            // Solo añadir si el tren del día siguiente sale dentro de 24h desde depMin
            if (newDep - depMin > 1440) continue;
            extended.add(c.toBuilder()
                    .departureTime(formatExtendedTime(newDep))
                    .arrivalTime(formatExtendedTime(newArr))
                    .build());
        }
        extended.sort(Comparator.comparingInt(c -> toMinutes(c.getDepartureTime())));
        return extended;
    }

    private static String formatExtendedTime(int minutes) {
        // Formato especial para tiempos extendidos: "26:30:00" indica día siguiente
        return String.format("%02d:%02d:00", minutes / 60, minutes % 60);
    }

    public double probCatchWithRT(String tripId, int depMin, int arrivalAtStop) {
        int slack = depMin - arrivalAtStop;
        if (slack < 0) return 0.0;

        String tipo = tripToTipo.get(tripId);
        if (tipo == null) return 1.0;

        double[] params = reliabilityParams.get(tipo);
        if (params == null) return 1.0;

        double mu = params[0];
        // Si hay dato RT, reducimos sigma a la mitad — menos incertidumbre
        boolean hasRT = realtimeDelayService.hasRealtimeData(tripId);
        double sigma = hasRT ? params[1] / 2.0 : params[1];

        double z = (Math.log(Math.max(slack, 0.001)) - mu) / sigma;
        return 1.0 - normalCDF(z);
    }
}