package com.github.davidsanzmartinez.planificadorderutes.application.usecase;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopRepository;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing.CsaMeatJourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing.DijkstraJourneyPlanner;
import com.github.davidsanzmartinez.planificadorderutes.infraestructure.routing.McRaptorJourneyPlanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SearchJourneyUseCaseTest {

    private DijkstraJourneyPlanner dijkstra;
    private CsaMeatJourneyPlanner csaMeat;
    private McRaptorJourneyPlanner mcRaptor;
    private StopRepository stopRepository;
    private GetJourneyDetailsUseCase getJourneyDetailsUseCase;
    private SearchJourneyUseCase service;

    @BeforeEach
    void setup() {
        dijkstra = mock(DijkstraJourneyPlanner.class);
        csaMeat = mock(CsaMeatJourneyPlanner.class);
        mcRaptor = mock(McRaptorJourneyPlanner.class);
        stopRepository = mock(StopRepository.class);
        getJourneyDetailsUseCase = mock(GetJourneyDetailsUseCase.class);

        service = new SearchJourneyUseCase(dijkstra, csaMeat, mcRaptor,
                stopRepository, getJourneyDetailsUseCase);

        // Stops vacíos
        when(stopRepository.findById(anyString())).thenReturn(Optional.empty());

        // Cada planner devuelve una lista vacía por defecto
        when(dijkstra.findJourneys(any(), any(), any(), any())).thenReturn(List.of());
        when(csaMeat.findJourneys(any(), any(), any(), any())).thenReturn(List.of());
        when(mcRaptor.findJourneys(any(), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void usaCsaMeatCuandoSeEspecifica() {
        service.execute("18000", "71801", LocalDate.of(2026, 6, 13),
                "12:00", "csa-meat");
        verify(csaMeat).findJourneys("18000", "71801",
                LocalDate.of(2026, 6, 13), "12:00");
        verify(dijkstra, never()).findJourneys(any(), any(), any(), any());
        verify(mcRaptor, never()).findJourneys(any(), any(), any(), any());
    }

    @Test
    void usaDijkstraCuandoSeEspecifica() {
        service.execute("18000", "71801", LocalDate.of(2026, 6, 13),
                "12:00", "dijkstra");
        verify(dijkstra).findJourneys(any(), any(), any(), any());
        verify(csaMeat, never()).findJourneys(any(), any(), any(), any());
        verify(mcRaptor, never()).findJourneys(any(), any(), any(), any());
    }

    @Test
    void usaMcRaptorCuandoSeEspecifica() {
        service.execute("18000", "71801", LocalDate.of(2026, 6, 13),
                "12:00", "mcraptor");
        verify(mcRaptor).findJourneys(any(), any(), any(), any());
    }

    @Test
    void usaMcRaptorPorDefectoSiNoSeEspecifica() {
        service.execute("18000", "71801", LocalDate.of(2026, 6, 13),
                "12:00", null);
        verify(mcRaptor).findJourneys(any(), any(), any(), any());
    }

    @Test
    void usaMcRaptorPorDefectoSiAlgoritmoDesconocido() {
        service.execute("18000", "71801", LocalDate.of(2026, 6, 13),
                "12:00", "algoritmo-inexistente");
        verify(mcRaptor).findJourneys(any(), any(), any(), any());
    }

    @Test
    void ignoraMayusculasMinusculasEnAlgoritmo() {
        service.execute("18000", "71801", LocalDate.of(2026, 6, 13),
                "12:00", "CSA-MEAT");
        verify(csaMeat).findJourneys(any(), any(), any(), any());
    }

    @Test
    void propagaCamposDelJourneyEnriquecido() {
        // Setup: planner devuelve un journey, useCase lo enriquece
        Journey rawJourney = Journey.builder()
                .departureTime("12:00:00")
                .arrivalTime("16:00:00")
                .totalDuration("4h")
                .numTransfers(1)
                .connections(List.of(Connection.builder().tripId("trip_1").build()))
                .transferReliability(0.85)
                .expectedDelayMinutes(5.0)
                .hasRealtimeData(true)
                .build();

        when(mcRaptor.findJourneys(any(), any(), any(), any()))
                .thenReturn(List.of(rawJourney));
        when(getJourneyDetailsUseCase.execute(any())).thenReturn(rawJourney);

        List<Journey> result = service.execute("18000", "71801",
                LocalDate.of(2026, 6, 13), "12:00", "mcraptor");

        assertEquals(1, result.size());
        Journey r = result.get(0);
        assertEquals(0.85, r.getTransferReliability());
        assertEquals(5.0, r.getExpectedDelayMinutes());
        assertTrue(r.isHasRealtimeData());
    }
}