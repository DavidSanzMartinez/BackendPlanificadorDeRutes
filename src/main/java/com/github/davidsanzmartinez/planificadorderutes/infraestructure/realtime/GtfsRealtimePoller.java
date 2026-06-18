package com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsRealtimePoller {

    private static final String FEED_CERCANIAS = "https://gtfsrt.renfe.com/trip_updates.json";
    private static final String FEED_LD = "https://gtfsrt.renfe.com/trip_updates_LD.json";
    private static final Duration TTL = Duration.ofSeconds(90);

    private final StringRedisTemplate redis;
    private final RestClient restClient = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory())
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 30000)
    public void poll() {
        log.debug("Polling GTFS-RT feeds...");
        try {
            processFeed(FEED_CERCANIAS);
            processFeed(FEED_LD);
        } catch (Exception e) {
            log.warn("Error polling GTFS-RT feeds: {}", e.getMessage());
        }
    }


    private void processFeed(String url) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String json = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

                if (json == null || json.isBlank()) return;

                JsonNode root = objectMapper.readTree(json);
                JsonNode entities = root.path("entity");

                int updated = 0;
                for (JsonNode entity : entities) {
                    JsonNode tripUpdate = entity.path("tripUpdate");
                    if (tripUpdate.isMissingNode()) continue;

                    String tripId = tripUpdate.path("trip").path("tripId").asText();
                    if (tripId.isBlank()) continue;

                    String scheduleRelationship = tripUpdate.path("trip")
                            .path("scheduleRelationship").asText("SCHEDULED");

                    // Cancelaciones
                    if ("CANCELED".equals(scheduleRelationship)) {
                        redis.opsForValue().set("trip_cancelled:" + tripId, "true", TTL);
                        continue;
                    }

                    // Retraso global del viaje
                    int delay = tripUpdate.path("delay").asInt(0);
                    redis.opsForValue().set("trip_delay:" + tripId,
                            String.valueOf(delay), TTL);

                    // Retraso por parada (parada actual)
                    JsonNode stopUpdates = tripUpdate.path("stopTimeUpdate");
                    for (JsonNode stopUpdate : stopUpdates) {
                        String stopId = stopUpdate.path("stopId").asText();
                        if (stopId.isBlank()) continue;
                        int stopDelay = stopUpdate.path("arrival").path("delay").asInt(delay);
                        redis.opsForValue().set(
                                "stop_delay:" + tripId + ":" + stopId,
                                String.valueOf(stopDelay), TTL);
                    }
                    updated++;
                }
                log.debug("Processed {} trip updates from {}", updated, url);
                return;

            } catch (Exception e) {
                if (attempt == 2) {
                    log.warn("Error processing feed {} after 3 attempts: {}", url, e.getMessage());
                }
            }
        }
    }
}