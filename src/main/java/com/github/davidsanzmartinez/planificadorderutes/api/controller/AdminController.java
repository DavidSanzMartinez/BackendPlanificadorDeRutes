package com.github.davidsanzmartinez.planificadorderutes.api.controller;

import com.github.davidsanzmartinez.planificadorderutes.api.dto.GtfsLoadRequest;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.gtfs.GtfsLoader;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.gtfs.GtfsScheduler;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final GtfsLoader gtfsLoader;
    private final StopJpaRepository stopRepository;
    private final RouteJpaRepository routeRepository;
    private final CalendarJpaRepository calendarRepository;
    private final TripJpaRepository tripRepository;
    private final StopTimeJpaRepository stopTimeRepository;
    private final CalendarDateJpaRepository calendarDateRepository;
    private final FootpathJpaRepository footpathRepository;
    private final TransferTimeJpaRepository transferTimeRepository;
    private final GtfsScheduler gtfsScheduler;

    @GetMapping("/memory")
    public Map<String, Long> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        return Map.of(
                "usedMB", usedMemory,
                "totalMB", totalMemory,
                "maxMB", maxMemory,
                "freeMB", maxMemory - usedMemory
        );
    }

    @PostMapping("load-gtfs")
    public ResponseEntity<String> loadGtfs(@RequestBody GtfsLoadRequest request) {
        try {
            log.info("Load GTFS request received");
            clearData();
            if (request.getDirectory2() != null && !request.getDirectory2().isBlank()) {
                gtfsLoader.loadFromMultiple(Path.of(request.getDirectory1()),
                        Path.of(request.getDirectory2())
                );
            } else {
                gtfsLoader.load(Path.of(request.getDirectory1()));
            }
            return ResponseEntity.ok("GTFS data loaded completed correctly");
        } catch (Exception e) {
            log.error("Error while loading GTFS", e);
            return ResponseEntity.internalServerError()
                    .body("Error while loading: " + e.getMessage());
        }
    }

    @PostMapping("/download-and-reload-gtfs")
    public ResponseEntity<String> downloadAndReloadGtfs() {
        try {
            log.info("Manual GTFS download and reload requested");
            gtfsScheduler.checkAndReload();
            return ResponseEntity.ok("Download and reload completed");
        } catch (Exception e) {
            log.error("Error during manual GTFS download and reload", e);
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }

    private void clearData() {
        log.info("Clearing existing data...");
        stopTimeRepository.deleteAll();
        tripRepository.deleteAll();
        calendarRepository.deleteAll();
        routeRepository.deleteAll();
        stopRepository.deleteAll();
        calendarDateRepository.deleteAll();
        footpathRepository.deleteAll();
        transferTimeRepository.deleteAll();
        log.info("Existing data cleared");
    }
}
