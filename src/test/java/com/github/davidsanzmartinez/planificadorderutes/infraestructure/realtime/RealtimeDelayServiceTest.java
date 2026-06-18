package com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimeDelayServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private RealtimeDelayService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new RealtimeDelayService(redis);
    }

    // ── getDelaySeconds ──

    @Test
    void getDelaySeconds_tripConRetraso() {
        when(ops.get("trip_delay:trip_1")).thenReturn("300");
        assertEquals(300, service.getDelaySeconds("trip_1"));
    }

    @Test
    void getDelaySeconds_tripSinRetraso() {
        when(ops.get("trip_delay:trip_x")).thenReturn(null);
        assertEquals(0, service.getDelaySeconds("trip_x"));
    }

    @Test
    void getDelaySeconds_retrasoNegativoDevuelveCero() {
        // Adelanto en RT (delay negativo) se trata como 0 para routing
        when(ops.get("trip_delay:trip_neg")).thenReturn("-60");
        assertEquals(0, service.getDelaySeconds("trip_neg"));
    }

    @Test
    void getDelaySeconds_errorDevuelveCero() {
        when(ops.get(anyString())).thenThrow(new RuntimeException("redis down"));
        assertEquals(0, service.getDelaySeconds("trip_1"));
    }

    // ── isCancelled ──

    @Test
    void isCancelled_tripCancelado() {
        when(ops.get("trip_cancelled:trip_1")).thenReturn("true");
        assertTrue(service.isCancelled("trip_1"));
    }

    @Test
    void isCancelled_tripNoCancelado() {
        when(ops.get("trip_cancelled:trip_1")).thenReturn(null);
        assertFalse(service.isCancelled("trip_1"));
    }

    // ── getRealtimeInfo ──

    @Test
    void getRealtimeInfo_walkDevuelveNulls() {
        RealtimeDelayService.RealtimeInfo info = service.getRealtimeInfo("WALK");
        assertNull(info.delayMinutes());
        assertNull(info.cancelled());
    }

    @Test
    void getRealtimeInfo_tripConRetraso() {
        when(ops.get("trip_delay:trip_1")).thenReturn("300");
        when(ops.get("trip_cancelled:trip_1")).thenReturn(null);
        RealtimeDelayService.RealtimeInfo info = service.getRealtimeInfo("trip_1");
        assertEquals(5, info.delayMinutes());  // 300s / 60 = 5min
        assertNull(info.cancelled());
    }

    @Test
    void getRealtimeInfo_tripCancelado() {
        when(ops.get("trip_delay:trip_1")).thenReturn(null);
        when(ops.get("trip_cancelled:trip_1")).thenReturn("true");
        RealtimeDelayService.RealtimeInfo info = service.getRealtimeInfo("trip_1");
        assertNull(info.delayMinutes());
        assertTrue(info.cancelled());
    }

    @Test
    void getRealtimeInfo_sinDatosDevuelveNulls() {
        when(ops.get(anyString())).thenReturn(null);
        RealtimeDelayService.RealtimeInfo info = service.getRealtimeInfo("trip_x");
        assertNull(info.delayMinutes());
        assertNull(info.cancelled());
    }

    @Test
    void getRealtimeInfo_retrasoNegativoDevuelveCero() {
        when(ops.get("trip_delay:trip_neg")).thenReturn("-60");
        when(ops.get("trip_cancelled:trip_neg")).thenReturn(null);
        RealtimeDelayService.RealtimeInfo info = service.getRealtimeInfo("trip_neg");
        assertEquals(0, info.delayMinutes());  // negativo → 0
    }

    // ── getDelaysForTrips (MGET masivo) ──

    @Test
    void getDelaysForTrips_listaVaciaDevuelveMapaVacio() {
        assertTrue(service.getDelaysForTrips(List.of()).isEmpty());
    }

    @Test
    void getDelaysForTrips_variosTripsConRetraso() {
        when(ops.multiGet(List.of("trip_delay:t1", "trip_delay:t2", "trip_delay:t3")))
                .thenReturn(List.of("300", "120", "60"));
        var result = service.getDelaysForTrips(List.of("t1", "t2", "t3"));
        assertEquals(5, result.get("t1"));   // 300/60
        assertEquals(2, result.get("t2"));   // 120/60
        assertEquals(1, result.get("t3"));   // 60/60
    }

    @Test
    void getDelaysForTrips_algunosNullSeOmiten() {
        when(ops.multiGet(anyList()))
                .thenReturn(java.util.Arrays.asList("300", null, "120"));
        var result = service.getDelaysForTrips(List.of("t1", "t2", "t3"));
        assertEquals(5, result.get("t1"));
        assertNull(result.get("t2"));  // no estaba en Redis
        assertEquals(2, result.get("t3"));
    }
}