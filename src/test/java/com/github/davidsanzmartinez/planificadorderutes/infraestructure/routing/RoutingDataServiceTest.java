package com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing;

import com.github.davidsanzmartinez.planificadorderutes.infraestructure.realtime.RealtimeDelayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class RoutingDataServiceTest {

    private RoutingDataService service;

    @BeforeEach
    void setup() {
        service = new RoutingDataService(null, null, null, null, null, null, null, null);
        Map<String, String> tripToTipo = new HashMap<>();
        tripToTipo.put("trip_ave_1", "AVE");
        tripToTipo.put("trip_md_1", "MD");
        tripToTipo.put("trip_sin_tipo", null);
        tripToTipo.put("trip_ave_con_rt", "AVE");
        tripToTipo.put("trip_ave_sin_rt", "AVE");
        Map<String, double[]> params = new HashMap<>();
        params.put("AVE", new double[]{1.690, 1.112, 8.92});
        params.put("MD", new double[]{1.632, 1.115, 9.12});
        ReflectionTestUtils.setField(service, "tripToTipo", tripToTipo);
        ReflectionTestUtils.setField(service, "reliabilityParams", params);
        RealtimeDelayService mockRT = Mockito.mock(RealtimeDelayService.class);
        Mockito.when(mockRT.hasRealtimeData("trip_ave_con_rt")).thenReturn(true);
        Mockito.when(mockRT.hasRealtimeData("trip_ave_sin_rt")).thenReturn(false);
        ReflectionTestUtils.setField(service, "realtimeDelayService", mockRT);
    }

    // ── Métodos estáticos de utilidad ──

    @Test
    void toMinutes_horaNormal() {
        assertEquals(0, RoutingDataService.toMinutes("00:00:00"));
        assertEquals(60, RoutingDataService.toMinutes("01:00:00"));
        assertEquals(720, RoutingDataService.toMinutes("12:00:00"));
        assertEquals(1439, RoutingDataService.toMinutes("23:59:00"));
    }

    @Test
    void toMinutes_horaExtendidaMultidia() {
        // Tiempos > 24:00 representan día siguiente
        assertEquals(1440, RoutingDataService.toMinutes("24:00:00"));
        assertEquals(1590, RoutingDataService.toMinutes("26:30:00"));
        assertEquals(2070, RoutingDataService.toMinutes("34:30:00"));
    }

    @Test
    void toMinutes_valoresInvalidos() {
        assertEquals(0, RoutingDataService.toMinutes(null));
        assertEquals(0, RoutingDataService.toMinutes(""));
        assertEquals(0, RoutingDataService.toMinutes("invalid"));
    }

    @Test
    void minutesToHhmm_horaNormal() {
        assertEquals("00:00:00", RoutingDataService.minutesToHhmm(0));
        assertEquals("01:00:00", RoutingDataService.minutesToHhmm(60));
        assertEquals("12:30:00", RoutingDataService.minutesToHhmm(750));
        assertEquals("23:59:00", RoutingDataService.minutesToHhmm(1439));
    }

    @Test
    void minutesToHhmm_horaExtendidaMultidia() {
        assertEquals("24:00:00", RoutingDataService.minutesToHhmm(1440));
        assertEquals("26:30:00", RoutingDataService.minutesToHhmm(1590));
    }

    @Test
    void toMinutesYminutesToHhmm_sonInversos() {
        for (int min : new int[]{0, 60, 720, 1439, 1590, 2070}) {
            assertEquals(min, RoutingDataService.toMinutes(
                    RoutingDataService.minutesToHhmm(min)));
        }
    }

    // ── probCatch ──

    @Test
    void probCatch_slackNegativoDevuelveCero() {
        // Si el slack es negativo, no se puede coger el tren
        assertEquals(0.0, service.probCatch("trip_ave_1", 700, 720));
    }

    @Test
    void probCatch_tripIdDesconocidoDevuelveUno() {
        // Sin información de tipo, asume fiabilidad máxima
        assertEquals(1.0, service.probCatch("trip_inexistente", 720, 700));
    }

    @Test
    void probCatch_aveConSlackAmplio() {
        // Slack de 30 min para AVE → probabilidad alta
        double p = service.probCatch("trip_ave_1", 750, 720);
        assertTrue(p > 0.05 && p < 0.20,
                "probCatch AVE slack=30 debería estar entre 0.05 y 0.20, fue " + p);
    }

    @Test
    void probCatch_aveConSlackAjustado() {
        // Slack de 5 min para AVE → probabilidad muy baja
        double p = service.probCatch("trip_ave_1", 725, 720);
        assertTrue(p > 0.5,
                "probCatch AVE slack=5 debería ser > 0.5, fue " + p);
    }

    @Test
    void probCatch_masSlackImplicaMayorProbabilidad() {
        // Monotonía: a más slack, menor riesgo de perder el tren (probCatch más bajo)
        double pSlack5 = service.probCatch("trip_ave_1", 725, 720);
        double pSlack15 = service.probCatch("trip_ave_1", 735, 720);
        double pSlack30 = service.probCatch("trip_ave_1", 750, 720);
        assertTrue(pSlack5 > pSlack15);
        assertTrue(pSlack15 > pSlack30);
    }

    // ── getRetrasoMedio ──

    @Test
    void getRetrasoMedio_tripConTipo() {
        assertEquals(8.92, service.getRetrasoMedio("trip_ave_1"));
        assertEquals(9.12, service.getRetrasoMedio("trip_md_1"));
    }

    @Test
    void getRetrasoMedio_tripSinTipoDevuelveCero() {
        assertEquals(0.0, service.getRetrasoMedio("trip_inexistente"));
    }

    @Test
    void probCatchWithRT_sinDatosRTUsaSigmaCompleta() {
        // Sin RT, comportamiento igual a probCatch normal
        double pNormal = service.probCatch("trip_ave_sin_rt", 735, 720);
        double pRT = service.probCatchWithRT("trip_ave_sin_rt", 735, 720);
        assertEquals(pNormal, pRT, 0.0001);
    }

    @Test
    void probCatchWithRT_conDatosRTUsaSigmaReducida() {
        // Con RT, sigma se reduce a la mitad → menos incertidumbre
        // Mismo slack pero distinto sigma da resultados distintos
        double pSinRT = service.probCatchWithRT("trip_ave_sin_rt", 735, 720);
        double pConRT = service.probCatchWithRT("trip_ave_con_rt", 735, 720);
        assertNotEquals(pSinRT, pConRT, 0.001,
                "Con RT debe usar sigma distinto y dar resultado distinto");
    }

    @Test
    void probCatchWithRT_slackNegativoDevuelveCero() {
        assertEquals(0.0, service.probCatchWithRT("trip_ave_con_rt", 700, 720));
    }

    @Test
    void nucleoMasCercano_paradasMadrid() {
        Map<String, double[]> stopCoords = Map.of(
                "60000", new double[]{40.406, -3.690},  // Madrid-Atocha
                "17000", new double[]{40.471, -3.683}   // Madrid-Chamartín
        );
        Map<String, double[]> centroides = Map.of(
                "MADRID",    new double[]{40.40, -3.70},
                "BARCELONA", new double[]{41.40,  2.10},
                "SEVILLA",   new double[]{37.38, -5.98}
        );
        String nucleo = service.nucleoMasCercano(
                List.of("60000", "17000"), stopCoords, centroides);
        assertEquals("MADRID", nucleo);
    }

    @Test
    void nucleoMasCercano_paradasBarcelona() {
        Map<String, double[]> stopCoords = Map.of(
                "71801", new double[]{41.380, 2.141},  // Barcelona-Sants
                "79200", new double[]{41.772, 2.674}   // Maçanet
        );
        Map<String, double[]> centroides = Map.of(
                "MADRID",    new double[]{40.40, -3.70},
                "BARCELONA", new double[]{41.40,  2.10},
                "SEVILLA",   new double[]{37.38, -5.98}
        );
        String nucleo = service.nucleoMasCercano(
                List.of("71801", "79200"), stopCoords, centroides);
        assertEquals("BARCELONA", nucleo);
    }

    @Test
    void nucleoMasCercano_listaVaciaDevuelveNull() {
        String nucleo = service.nucleoMasCercano(List.of(), Map.of(), Map.of());
        assertNull(nucleo);
    }

    @Test
    void nucleoMasCercano_paradasSinCoordenadasDevuelveNull() {
        Map<String, double[]> stopCoords = Map.of();  // sin coords
        Map<String, double[]> centroides = Map.of(
                "MADRID", new double[]{40.40, -3.70}
        );
        String nucleo = service.nucleoMasCercano(
                List.of("desconocido"), stopCoords, centroides);
        assertNull(nucleo);
    }
}