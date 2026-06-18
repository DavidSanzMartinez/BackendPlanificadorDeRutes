package com.github.davidsanzmartinez.planificadorderutes.infraestructure.gtfs;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.*;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsLoader {
    private final StopJpaRepository stopJpaRepository;
    private final TripJpaRepository tripJpaRepository;
    private final CalendarJpaRepository calendarJpaRepository;
    private final StopTimeJpaRepository stopTimeJpaRepository;
    private final RouteJpaRepository routeJpaRepository;
    private final CalendarDateJpaRepository calendarDateJpaRepository;
    private final FootpathJpaRepository footpathJpaRepository;
    private final TransferTimeJpaRepository transferTimeJpaRepository;

    public void load(Path gtfsDirectory) throws IOException, CsvValidationException {
        log.info("Loading GTFS data from {}", gtfsDirectory);
        loadStops(gtfsDirectory.resolve("stops.txt"));
        loadRoutes(gtfsDirectory.resolve("routes.txt"));
        loadCalendar(gtfsDirectory.resolve("calendar.txt"));
        loadTrips(gtfsDirectory.resolve("trips.txt"));
        loadStopTimes(gtfsDirectory.resolve("stop_times.txt"));
        loadCalendarDates(gtfsDirectory.resolve("calendar_dates.txt"));
        loadTransfers(gtfsDirectory.resolve("transfers.txt"));
        generateFootpaths();
        log.info("GTFS data loaded");
    }

    public void loadFromMultiple(Path gtfsDirectory1, Path gtfsDirectory2) throws IOException, CsvValidationException {
        log.info("Loading GTFS data from 2 sources");
        loadStops(gtfsDirectory1.resolve("stops.txt"), gtfsDirectory2.resolve("stops.txt"));
        loadRoutes(gtfsDirectory1.resolve("routes.txt"), gtfsDirectory2.resolve("routes.txt"));
        loadCalendar(gtfsDirectory1.resolve("calendar.txt"), gtfsDirectory2.resolve("calendar.txt"));
        loadTrips(gtfsDirectory1.resolve("trips.txt"), gtfsDirectory2.resolve("trips.txt"));
        loadStopTimes(gtfsDirectory1.resolve("stop_times.txt"), gtfsDirectory2.resolve("stop_times.txt"));
        loadCalendarDates(
                gtfsDirectory1.resolve("calendar_dates.txt"),
                gtfsDirectory2.resolve("calendar_dates.txt")
        );
        loadTransfers(
                gtfsDirectory1.resolve("transfers.txt"),
                gtfsDirectory2.resolve("transfers.txt")
        );
        generateFootpaths();
        log.info("GTFS data loaded");
    }

    private void loadStops(Path... files) throws IOException, CsvValidationException {
        log.info("Loading stops...");
        List<StopEntity> entities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Path file : files) {
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String stopId = get(line, idx, "stop_id");
                    if (stopId.isBlank() || seenIds.contains(stopId)) continue;
                    seenIds.add(stopId);
                    entities.add(StopEntity.builder()
                            .stopId(stopId)
                            .stopName(get(line, idx, "stop_name"))
                            .stopLat(getDouble(line, idx, "stop_lat"))
                            .stopLon(getDouble(line, idx, "stop_lon"))
                            .wheelchairBoarding(getInt(line, idx, "wheelchair_boarding"))
                            .build());
                }
            }
        }
        stopJpaRepository.saveAll(entities);
        log.info("{} Stops loaded", entities.size());
    }

    private void loadRoutes(Path... files) throws IOException, CsvValidationException{
        log.info("Loading routes...");
        List<RouteEntity> entities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Path file : files) {
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String routeId = get(line, idx, "route_id");
                    if (routeId.isBlank() || seenIds.contains(routeId)) continue;
                    seenIds.add(routeId);
                    entities.add(RouteEntity.builder()
                            .routeId(routeId)
                            .routeShortName(get(line, idx, "route_short_name"))
                            .routeLongName(get(line, idx, "route_long_name"))
                            .routeType(getInt(line, idx, "route_type"))
                            .routeColor(get(line, idx, "route_color"))
                            .routeTextColor(get(line, idx, "route_text_color"))
                            .build());
                }
            }
        }
        routeJpaRepository.saveAll(entities);
        log.info("{} Routes loaded", entities.size());
    }

    private void loadCalendar(Path... files) throws IOException, CsvValidationException {
        log.info("Loading calendar...");
        List<CalendarEntity> entities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Path file : files) {
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String serviceId = get(line, idx, "service_id");
                    if (serviceId.isBlank() || seenIds.contains(serviceId)) continue;
                    seenIds.add(serviceId);
                    entities.add(CalendarEntity.builder()
                            .serviceId(serviceId)
                            .monday(getBool(line, idx, "monday"))
                            .tuesday(getBool(line, idx, "tuesday"))
                            .wednesday(getBool(line, idx, "wednesday"))
                            .thursday(getBool(line, idx, "thursday"))
                            .friday(getBool(line, idx, "friday"))
                            .saturday(getBool(line, idx, "saturday"))
                            .sunday(getBool(line, idx, "sunday"))
                            .startDate(getDate(line, idx, "start_date"))
                            .endDate(getDate(line, idx, "end_date"))
                            .build());
                }
            }
        }
        calendarJpaRepository.saveAll(entities);
        log.info("{} Calendar entries loaded", entities.size());
    }

    private void loadTrips(Path... files) throws IOException, CsvValidationException {
        log.info("Loading trips...");
        List<TripEntity> entities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Path file : files) {
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String tripId = get(line, idx, "trip_id");
                    if (tripId.isBlank() || seenIds.contains(tripId)) continue;
                    seenIds.add(tripId);
                    entities.add(TripEntity.builder()
                            .tripId(tripId)
                            .routeId(get(line, idx, "route_id"))
                            .serviceId(get(line, idx, "service_id"))
                            .tripHeadsign(get(line, idx, "trip_headsign"))
                            .wheelchairAccessible(getInt(line, idx, "wheelchair_accessible"))
                            .blockId(get(line, idx, "block_id"))
                            .build());
                }
            }
        }
        tripJpaRepository.saveAll(entities);
        log.info("{} Trips loaded", entities.size());
    }

    private void loadStopTimes(Path... files) throws IOException, CsvValidationException {
        log.info("Loading stop_times...");
        int total = 0;
        Set<StopTimeId> seenIds = new HashSet<>();
        for (Path file : files) {
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                List<StopTimeEntity> batch = new ArrayList<>();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    StopTimeId id = new StopTimeId(
                            get(line, idx, "trip_id"),
                            getInt(line, idx, "stop_sequence")
                    );
                    if (seenIds.contains(id)) continue;
                    seenIds.add(id);
                    batch.add(StopTimeEntity.builder()
                            .id(id)
                            .arrivalTime(get(line, idx, "arrival_time"))
                            .departureTime(get(line, idx, "departure_time"))
                            .stopId(get(line, idx, "stop_id"))
                            .pickupType(getInt(line, idx, "pickup_type"))
                            .dropOffType(getInt(line, idx, "drop_off_type"))
                            .build());
                    if (batch.size() == 1000) {
                        stopTimeJpaRepository.saveAll(batch);
                        total += batch.size();
                        batch.clear();
                        log.info("{} stop_times loaded...", total);
                    }
                }
                if (!batch.isEmpty()) {
                    stopTimeJpaRepository.saveAll(batch);
                    total += batch.size();
                }
            }
        }
        log.info("{} Stop_times loaded in total", total);
    }

    private void loadCalendarDates(Path... files) throws IOException, CsvValidationException{
        log.info("Loading calendar_dates...");
        List<CalendarDateEntity> entities = new ArrayList<>();
        Set<CalendarDateId> seenIds = new HashSet<>();

        for (Path file : files) {
            if (!Files.exists(file)) {
                log.info("No calendar_dates.txt found in {}", file.getParent());
                continue;
            }
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String serviceId = get(line, idx, "service_id");
                    String dateStr = get(line, idx, "date");
                    if (serviceId.isBlank() || dateStr.isBlank()) continue;

                    LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.BASIC_ISO_DATE);
                    CalendarDateId id = new CalendarDateId(serviceId, date);

                    if (seenIds.contains(id)) continue;
                    seenIds.add(id);

                    entities.add(CalendarDateEntity.builder()
                            .id(id)
                            .exceptionType(getInt(line, idx, "exception_type"))
                            .build());
                }
            }
        }

        calendarDateJpaRepository.saveAll(entities);
        log.info("{} calendar_dates loaded", entities.size());
    }

    private void loadTransfers(Path... files) throws IOException, CsvValidationException {
        log.info("Loading transfers...");
        List<FootpathEntity> footpaths = new ArrayList<>();
        List<TransferTimeEntity> transferTimes = new ArrayList<>();
        Set<FootpathId> seenFootpaths = new HashSet<>();
        Set<TransferTimeId> seenTransferTimes = new HashSet<>();

        for (Path file : files) {
            if (!Files.exists(file)) {
                log.info("No transfers.txt found in {}", file.getParent());
                continue;
            }
            try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file))
                    .withSkipLines(0).build()) {
                Map<String, Integer> idx = readHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String fromStopId = get(line, idx, "from_stop_id");
                    String toStopId = get(line, idx, "to_stop_id");
                    String fromRouteId = get(line, idx, "from_route_id");
                    String toRouteId = get(line, idx, "to_route_id");
                    int transferType = getInt(line, idx, "transfer_type");
                    int minTransferTime = getInt(line, idx, "min_transfer_time");

                    if (fromStopId.isBlank() || toStopId.isBlank()) continue;

                    if (fromStopId.equals(toStopId)) {
                        // Transfer en la misma parada entre rutas
                        TransferTimeId id = new TransferTimeId(
                                fromStopId, toStopId, fromRouteId, toRouteId);
                        if (seenTransferTimes.contains(id)) continue;
                        seenTransferTimes.add(id);
                        transferTimes.add(TransferTimeEntity.builder()
                                .id(id)
                                .transferType(transferType)
                                .minTransferTime(minTransferTime)
                                .build());
                    } else {
                        // Footpath entre stops distintos
                        FootpathId id = new FootpathId(fromStopId, toStopId);
                        if (seenFootpaths.contains(id)) continue;
                        seenFootpaths.add(id);
                        footpaths.add(FootpathEntity.builder()
                                .id(id)
                                .transferTime(minTransferTime)
                                .distanceMeters(null)
                                .build());
                    }
                }
            }
        }

        footpathJpaRepository.saveAll(footpaths);
        transferTimeJpaRepository.saveAll(transferTimes);
        log.info("{} footpaths and {} transfer times loaded from transfers.txt",
                footpaths.size(), transferTimes.size());
    }

    private void generateFootpaths() {
        log.info("Generating footpaths by proximity...");

        List<StopEntity> stops = stopJpaRepository.findAll();
        Set<FootpathId> existingFootpaths = footpathJpaRepository.findAll()
                .stream()
                .map(FootpathEntity::getId)
                .collect(Collectors.toSet());

        List<FootpathEntity> newFootpaths = new ArrayList<>();
        double maxDistanceMeters = 300.0;

        for (int i = 0; i < stops.size(); i++) {
            StopEntity a = stops.get(i);
            for (int j = i + 1; j < stops.size(); j++) {
                StopEntity b = stops.get(j);

                double distance = haversineMeters(
                        a.getStopLat(), a.getStopLon(),
                        b.getStopLat(), b.getStopLon()
                );

                if (distance > maxDistanceMeters) continue;

                // Tiempo = distancia / 1 m/s + 120 segundos de margen
                int transferTime = (int) (distance / 1.0) + 120;

                FootpathId idAB = new FootpathId(
                        String.valueOf(a.getStopId()),
                        String.valueOf(b.getStopId())
                );
                FootpathId idBA = new FootpathId(
                        String.valueOf(b.getStopId()),
                        String.valueOf(a.getStopId())
                );

                if (!existingFootpaths.contains(idAB)) {
                    newFootpaths.add(FootpathEntity.builder()
                            .id(idAB)
                            .transferTime(transferTime)
                            .distanceMeters(distance)
                            .build());
                    existingFootpaths.add(idAB);
                }

                if (!existingFootpaths.contains(idBA)) {
                    newFootpaths.add(FootpathEntity.builder()
                            .id(idBA)
                            .transferTime(transferTime)
                            .distanceMeters(distance)
                            .build());
                    existingFootpaths.add(idBA);
                }
            }
        }

        footpathJpaRepository.saveAll(newFootpaths);
        log.info("{} footpaths generated by proximity", newFootpaths.size());
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Map<String, Integer> readHeader(CSVReader reader) throws IOException, CsvValidationException {
        String[] header = reader.readNext();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            idx.put(header[i].trim(), i);
        }
        return idx;
    }

    private String get(String[] line, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        return (i != null && i < line.length) ? line[i].trim() : "";
    }

    private int getInt(String[] line, Map<String, Integer> idx, String col) {
        String val = get(line, idx, col);
        return val.isBlank() ? 0 : Integer.parseInt(val);
    }

    private double getDouble(String[] line, Map<String, Integer> idx, String col) {
        String val = get(line, idx, col);
        return val.isBlank() ? 0.0 : Double.parseDouble(val);
    }

    private boolean getBool(String[] line, Map<String, Integer> idx, String col) {
        return get(line, idx, col).equals("1");
    }

    private LocalDate getDate(String[] line, Map<String, Integer> idx, String col) {
        String val = get(line, idx, col);
        return val.isBlank() ? null : LocalDate.parse(val, DateTimeFormatter.BASIC_ISO_DATE);
    }

}
