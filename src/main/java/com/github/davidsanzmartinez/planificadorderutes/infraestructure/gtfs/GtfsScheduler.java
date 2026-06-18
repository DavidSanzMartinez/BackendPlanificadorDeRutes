package com.github.davidsanzmartinez.planificadorderutes.infraestructure.gtfs;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity.GtfsMetadataEntity;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.repository.*;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsScheduler {
    private final GtfsLoader gtfsLoader;
    private final GtfsMetadataJpaRepository metadataRepository;
    private final StopJpaRepository stopRepository;
    private final RouteJpaRepository routeRepository;
    private final CalendarJpaRepository calendarRepository;
    private final TripJpaRepository tripRepository;
    private final StopTimeJpaRepository stopTimeRepository;
    private final CalendarDateJpaRepository calendarDateRepository;
    private final FootpathJpaRepository footpathRepository;
    private final TransferTimeJpaRepository transferTimeRepository;
    private final GtfsDownloader gtfsDownloader;

    @Value("${gtfs.directory1}")
    private String directory1;

    @Value("${gtfs.directory2}")
    private String directory2;

    @Value("${gtfs.download-url-cercanias}")
    private String downloadUrlCercanias;

    @Value("${gtfs.download-url-larga-distancia}")
    private String downloadUrlLargaDistancia;

    @Value("${gtfs.download-directory}")
    private String tempDirectory;

    @Value("${gtfs.download-enabled:true}")
    private boolean downloadEnabled;

    @Scheduled(cron = "${gtfs.cron}")
    public void checkAndReload() {
        log.info("Checking for GTFS updates...");
        try {
            if (downloadEnabled) {
                checkAndReloadFromWeb();
            } else {
                checkAndReloadFromLocal();
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Error during GTFS reload", e);
        }
    }

    private void checkAndReloadFromWeb() throws IOException, CsvValidationException {
        Path tempDir1 = Path.of(tempDirectory, "cercanias");
        Path tempDir2 = Path.of(tempDirectory, "larga-distancia");

        try {
            // Descarga los dos datasets
            gtfsDownloader.downloadAndExtract(downloadUrlCercanias, tempDir1);
            gtfsDownloader.downloadAndExtract(downloadUrlLargaDistancia, tempDir2);

            // Comprueba checksums
            if (hasChanges(tempDir1, tempDir2)) {
                log.info("Changes detected, reloading GTFS data...");
                clearData();
                gtfsLoader.loadFromMultiple(tempDir1, tempDir2);
                updateChecksums(tempDir1, tempDir2);
                log.info("Reload completed");
            } else {
                log.info("No changes detected in GTFS files");
            }
        } finally {
            // Borra ficheros temporales
            deleteTempDirectory(tempDir1);
            deleteTempDirectory(tempDir2);
        }
    }

    private void checkAndReloadFromLocal() throws IOException, CsvValidationException {
        if (hasChanges(Path.of(directory1), Path.of(directory2))) {
            log.info("Changes detected, reloading GTFS data...");
            clearData();
            gtfsLoader.loadFromMultiple(Path.of(directory1), Path.of(directory2));
            updateChecksums(Path.of(directory1), Path.of(directory2));
            log.info("Reload completed");
        } else {
            log.info("No changes detected in GTFS files");
        }
    }

    private void deleteTempDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.delete(path); }
                            catch (IOException e) { log.warn("Could not delete {}", path); }
                        });
            }
        } catch (IOException e) {
            log.warn("Could not delete temp directory {}", dir);
        }
    }

    private boolean hasChanges(Path dir1, Path dir2) throws IOException {
        List<Path> files = getTrackedFiles(dir1, dir2);
        for (Path file : files) {
            if (!Files.exists(file)) continue;
            String currentChecksum = computeChecksum(file);
            Optional<GtfsMetadataEntity> metadata = metadataRepository
                    .findByFileName(file.getFileName().toString());
            if (metadata.isEmpty() || !metadata.get().getChecksum().equals(currentChecksum)) {
                return true;
            }
        }
        return false;
    }

    private void updateChecksums(Path dir1, Path dir2) throws IOException {
        List<Path> files = getTrackedFiles(dir1, dir2);
        for (Path file : files) {
            if (!Files.exists(file)) continue;
            String checksum = computeChecksum(file);
            String fileName = file.getFileName().toString();
            GtfsMetadataEntity metadata = metadataRepository.findByFileName(fileName)
                    .orElse(GtfsMetadataEntity.builder().fileName(fileName).build());
            metadata.setChecksum(checksum);
            metadata.setLoadedAt(LocalDateTime.now());
            metadataRepository.save(metadata);
        }
    }

    private void clearData() {
        log.info("Clearing existing data...");
        stopTimeRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        calendarDateRepository.deleteAllInBatch();
        calendarRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        stopRepository.deleteAllInBatch();
        footpathRepository.deleteAllInBatch();
        transferTimeRepository.deleteAllInBatch();
        metadataRepository.deleteAllInBatch();
        log.info("Existing data cleared");
    }

    private String computeChecksum(Path file) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private List<Path> getTrackedFiles(Path dir1, Path dir2) {
        List<String> fileNames = List.of(
                "stops.txt", "routes.txt", "calendar.txt",
                "trips.txt", "stop_times.txt"
        );
        List<Path> paths = new ArrayList<>();
        for (String name : fileNames) {
            paths.add(dir1.resolve(name));
            paths.add(dir2.resolve(name));
        }
        return paths;
    }
}