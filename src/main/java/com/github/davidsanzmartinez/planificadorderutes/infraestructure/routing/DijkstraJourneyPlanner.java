package com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.JourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.CalendarEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.FootpathEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.StopTimeEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.TripEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.*;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime.RealtimeDelayService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DijkstraJourneyPlanner implements JourneyPlanner {

    private final StopTimeJpaRepository stopTimeRepository;
    private final TripJpaRepository tripRepository;
    private final CalendarJpaRepository calendarRepository;
    private final CalendarDateJpaRepository calendarDateRepository;

    private Map<String, List<Connection>> connectionsByDepartureStop = new HashMap<>();
    private Map<String, CalendarEntity> calendarByServiceId = new HashMap<>();
    private Map<String, String> serviceIdByTripId = new HashMap<>();
    private Map<String, Map<LocalDate, Integer>> calendarDateExceptions = new HashMap<>();

    private Map<String, List<FootpathEntity>> footpathsByDepartureStop = new HashMap<>();

    private final FootpathJpaRepository footpathRepository;

    private final RealtimeDelayService realtimeDelayService;

    @EventListener(ApplicationReadyEvent.class)
    public void buildIndex() {
        log.info("Building connection index for Dijkstra...");

        //Load exceptions from calendar_dates
        calendarDateExceptions = new HashMap<>();
        calendarDateRepository.findAll().forEach(cd -> {
            calendarDateExceptions
                    .computeIfAbsent(cd.getId().getServiceId(), k -> new HashMap<>())
                    .put(cd.getId().getDate(), cd.getExceptionType());
        });
        log.info("Loaded {} calendar_date exceptions for {} services",
                calendarDateRepository.count(),
                calendarDateExceptions.size());

        connectionsByDepartureStop = new HashMap<>();

        calendarByServiceId = calendarRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        CalendarEntity::getServiceId,
                        c -> c
                ));


        serviceIdByTripId = tripRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        TripEntity::getTripId,
                        TripEntity::getServiceId
                ));

        List<StopTimeEntity> allStopTimes = stopTimeRepository.findAllOrderedByDepartureTime();

        Map<String, List<StopTimeEntity>> byTrip = allStopTimes.stream()
                .collect(Collectors.groupingBy(st -> st.getId().getTripId()));

        for (Map.Entry<String, List<StopTimeEntity>> entry : byTrip.entrySet()) {
            List<StopTimeEntity> stopTimes = entry.getValue()
                    .stream()
                    .sorted(Comparator.comparingInt(st -> st.getId().getStopSequence()))
                    .toList();

            for (int i = 0; i < stopTimes.size() - 1; i++) {
                StopTimeEntity departure = stopTimes.get(i);
                StopTimeEntity arrival = stopTimes.get(i + 1);

                // No crear connexió si no es pot pujar a la parada de sortida
                if (departure.getPickupType() != null && departure.getPickupType() == 1) continue;
                // No crear connexió si no es pot baixar a la parada d'arribada
                if (arrival.getDropOffType() != null && arrival.getDropOffType() == 1) continue;


                Connection connection = Connection.builder()
                        .tripId(entry.getKey())
                        .departureStopId(departure.getStopId())
                        .arrivalStopId(arrival.getStopId())
                        .departureTime(departure.getDepartureTime())
                        .arrivalTime(arrival.getArrivalTime())
                        .departureSequence(departure.getId().getStopSequence())
                        .arrivalSequence(arrival.getId().getStopSequence())
                        .build();

                connectionsByDepartureStop
                        .computeIfAbsent(departure.getStopId(), k -> new ArrayList<>())
                        .add(connection);
            }
        }

        connectionsByDepartureStop.values().forEach(list ->
                list.sort(Comparator.comparing(c -> toMinutes(c.getDepartureTime()))));

        log.info("Connection index built with {} departure stops", connectionsByDepartureStop.size());

        // Carrega footpaths
        footpathsByDepartureStop = new HashMap<>();
        footpathRepository.findAll().forEach(fp ->
                footpathsByDepartureStop
                        .computeIfAbsent(fp.getId().getFromStopId(), k -> new ArrayList<>())
                        .add(fp)
        );
        log.info("Footpaths index built with {} departure stops", footpathsByDepartureStop.size());
    }

    private boolean operatesOn(String tripId, LocalDate date) {
        String serviceId = serviceIdByTripId.get(tripId);
        if (serviceId == null) return false;

        //Check excpetions from calendar_dates first
        Map<LocalDate, Integer> exceptions = calendarDateExceptions.get(serviceId);
        if (exceptions != null && exceptions.containsKey(date)) {
            int exceptionType = exceptions.get(date);
            // tipo 1 = service added that day, tipo 2 = service eliminated that day
            return exceptionType == 1;
        }

        //If there is no exception, use the base calendar
        CalendarEntity calendar = calendarByServiceId.get(serviceId);
        if (calendar == null) return false;

        if (date.isBefore(calendar.getStartDate()) || date.isAfter(calendar.getEndDate())) {
            return false;
        }

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

    @Override
    public List<Journey> findJourneys(String originStopId, String destinationStopId,
                                      LocalDate date, String departureTime) {
        int startMinutes = toMinutes(departureTime);

        Map<String, Integer> earliestArrival = new HashMap<>();
        Map<String, Connection> previousConnection = new HashMap<>();
        earliestArrival.put(originStopId, startMinutes);

        PriorityQueue<StopState> queue = new PriorityQueue<>(
                Comparator.comparingInt(s -> s.arrivalMinutes));
        queue.add(new StopState(originStopId, startMinutes));

        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            StopState current = queue.poll();

            if (visited.contains(current.stopId)) continue;
            visited.add(current.stopId);

            if (current.stopId.equals(destinationStopId)) break;

            List<Connection> connections = connectionsByDepartureStop
                    .getOrDefault(current.stopId, List.of());

            for (Connection conn : connections) {
                if (!operatesOn(conn.getTripId(), date)) continue;

                int delayMinutes = realtimeDelayService.getDelaySeconds(conn.getTripId()) / 60;
                int connDeparture = toMinutes(conn.getDepartureTime()) + delayMinutes;
                if (connDeparture < current.arrivalMinutes) continue;
                int arrivalMinutes = toMinutes(conn.getArrivalTime()) + delayMinutes;

                String nextStop = conn.getArrivalStopId();

                if (!earliestArrival.containsKey(nextStop) ||
                        arrivalMinutes < earliestArrival.get(nextStop)) {
                    earliestArrival.put(nextStop, arrivalMinutes);
                    previousConnection.put(nextStop, conn);
                    queue.add(new StopState(nextStop, arrivalMinutes));
                }
            }

            // Footpaths — transfers a peu
            for (FootpathEntity fp : footpathsByDepartureStop.getOrDefault(current.stopId, List.of())) {
                String nextStop = fp.getId().getToStopId();
                int walkArrival = current.arrivalMinutes + (fp.getTransferTime() / 60);

                if (!earliestArrival.containsKey(nextStop) ||
                        walkArrival < earliestArrival.get(nextStop)) {
                    earliestArrival.put(nextStop, walkArrival);

                    int walkMinutes = fp.getTransferTime() / 60;

                    previousConnection.put(nextStop, Connection.builder()
                            .tripId("WALK")
                            .departureStopId(current.stopId)
                            .arrivalStopId(nextStop)
                            .departureTime(minutesToHhmm(current.arrivalMinutes))
                            .arrivalTime(minutesToHhmm(walkArrival))
                            .departureSequence(0)
                            .arrivalSequence(0)
                            .walkingMinutes(walkMinutes)
                            .build());
                    queue.add(new StopState(nextStop, walkArrival));
                }
            }
        }

        if (!earliestArrival.containsKey(destinationStopId)) {
            return List.of();
        }

        List<Connection> path = reconstructPath(previousConnection, originStopId, destinationStopId);

        path = path.stream().map(c -> {
            RealtimeDelayService.RealtimeInfo rt = realtimeDelayService.getRealtimeInfo(c.getTripId());
            return c.toBuilder()
                    .realtimeDelayMinutes(rt.delayMinutes())
                    .cancelled(rt.cancelled())
                    .build();
        }).toList();

        boolean hasRealtimeData = path.stream()
                .anyMatch(c -> c.getRealtimeDelayMinutes() != null || c.getCancelled() != null);

        Journey journey = Journey.builder()
                .origin(null)
                .destination(null)
                .departureTime(path.get(0).getDepartureTime())
                .arrivalTime(path.get(path.size() - 1).getArrivalTime())
                .totalDuration(formatDuration(startMinutes,
                        toMinutes(path.get(path.size() - 1).getArrivalTime())))
                .numTransfers(countTransfers(path))
                .connections(path)
                .hasRealtimeData(hasRealtimeData)
                .build();

        return List.of(journey);
    }

    private List<Connection> reconstructPath(Map<String, Connection> previousConnection,
                                             String originStopId, String destinationStopId) {
        LinkedList<Connection> path = new LinkedList<>();
        String current = destinationStopId;
        while (previousConnection.containsKey(current)) {
            Connection conn = previousConnection.get(current);
            path.addFirst(conn);
            current = conn.getDepartureStopId();
        }
        return path;
    }

    private int countTransfers(List<Connection> connections) {
        if (connections.isEmpty()) return 0;
        int transfers = 0;
        String currentTrip = connections.get(0).getTripId();
        for (int i = 1; i < connections.size(); i++) {
            if (!connections.get(i).getTripId().equals(currentTrip)) {
                transfers++;
                currentTrip = connections.get(i).getTripId();
            }
        }
        return transfers;
    }

    private String formatDuration(int departureMinutes, int arrivalMinutes) {
        int duration = arrivalMinutes - departureMinutes;
        return (duration / 60) + "h " + (duration % 60) + "min";
    }

    public static int toMinutes(String time) {
        if (time == null || time.isBlank()) return 0;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private String minutesToHhmm(int minutes) {
        return String.format("%02d:%02d:00", minutes / 60, minutes % 60);
    }

    @Data
    @AllArgsConstructor
    private static class StopState {
        private String stopId;
        private int arrivalMinutes;
    }
}
