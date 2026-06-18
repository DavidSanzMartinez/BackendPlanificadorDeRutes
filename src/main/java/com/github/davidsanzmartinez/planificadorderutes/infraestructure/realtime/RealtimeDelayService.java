package com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeDelayService {

    private final StringRedisTemplate redis;

    public int getDelaySeconds(String tripId) {
        try {
            String val = redis.opsForValue().get("trip_delay:" + tripId);
            return val != null ? Math.max(0, Integer.parseInt(val)) : 0;
        } catch (Exception e) {
            log.warn("Error reading delay for trip {}: {}", tripId, e.getMessage());
            return 0;
        }
    }

    public boolean isCancelled(String tripId) {
        try {
            return "true".equals(redis.opsForValue().get("trip_cancelled:" + tripId));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasRealtimeData(String tripId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey("trip_delay:" + tripId))
                    || Boolean.TRUE.equals(redis.hasKey("trip_cancelled:" + tripId));
        } catch (Exception e) {
            return false;
        }
    }

    public record RealtimeInfo(Integer delayMinutes, Boolean cancelled) {}

    public RealtimeInfo getRealtimeInfo(String tripId) {
        if ("WALK".equals(tripId)) return new RealtimeInfo(null, null);
        try {
            String val = redis.opsForValue().get("trip_delay:" + tripId);
            Integer delay = val != null ? Math.max(0, Integer.parseInt(val) / 60) : null;
            Boolean cancelled = isCancelled(tripId) ? true : null;
            if (delay == null && cancelled == null) return new RealtimeInfo(null, null);
            return new RealtimeInfo(delay, cancelled);
        } catch (Exception e) {
            return new RealtimeInfo(null, null);
        }
    }

    // ── Carga masiva: una sola consulta MGET para todos los trips ──
    public Map<String, Integer> getDelaysForTrips(Collection<String> tripIds) {
        Map<String, Integer> result = new HashMap<>();
        if (tripIds == null || tripIds.isEmpty()) return result;
        try {
            List<String> keys = tripIds.stream()
                    .map(t -> "trip_delay:" + t)
                    .toList();
            List<String> ids = new ArrayList<>(tripIds);
            List<String> values = redis.opsForValue().multiGet(keys);
            if (values == null) return result;
            for (int i = 0; i < ids.size(); i++) {
                String val = values.get(i);
                if (val != null) {
                    try {
                        result.put(ids.get(i), Math.max(0, Integer.parseInt(val) / 60));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Error in bulk delay fetch: {}", e.getMessage());
        }
        return result;
    }
}