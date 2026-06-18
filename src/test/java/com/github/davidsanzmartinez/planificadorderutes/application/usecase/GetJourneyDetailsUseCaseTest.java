package com.github.davidsanzmartinez.planificadorderutes.application.usecase;

import com.github.davidsanzmartinez.planificadorderutes.domain.model.Connection;
import com.github.davidsanzmartinez.planificadorderutes.domain.model.Journey;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.RouteRepository;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.StopRepository;
import com.github.davidsanzmartinez.planificadorderutes.domain.port.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetJourneyDetailsUseCaseTest {

    private StopRepository stopRepository;
    private TripRepository tripRepository;
    private RouteRepository routeRepository;
    private GetJourneyDetailsUseCase service;

    @BeforeEach
    void setup() {
        stopRepository = mock(StopRepository.class);
        tripRepository = mock(TripRepository.class);
        routeRepository = mock(RouteRepository.class);
        when(stopRepository.findById(anyString())).thenReturn(Optional.empty());
        when(tripRepository.findById(anyString())).thenReturn(Optional.empty());
        service = new GetJourneyDetailsUseCase(
                stopRepository, tripRepository, routeRepository);
    }

    @Test
    void execute_propagaTransferReliabilityYExpectedDelay() {
        Journey input = Journey.builder()
                .departureTime("12:00:00")
                .arrivalTime("16:00:00")
                .totalDuration("4h")
                .numTransfers(1)
                .connections(List.of(
                        Connection.builder()
                                .tripId("trip_1")
                                .departureStopId("A")
                                .arrivalStopId("B")
                                .departureTime("12:00:00")
                                .arrivalTime("16:00:00")
                                .build()))
                .transferReliability(0.85)
                .expectedDelayMinutes(5.5)
                .hasRealtimeData(true)
                .build();

        Journey result = service.execute(input);

        assertEquals(0.85, result.getTransferReliability());
        assertEquals(5.5, result.getExpectedDelayMinutes());
        assertTrue(result.isHasRealtimeData());
    }

    @Test
    void execute_walkConservaCamposRT() {
        // Bug histórico: el WALK perdía realtimeDelayMinutes/cancelled al reconstruirse
        Journey input = Journey.builder()
                .departureTime("12:00:00")
                .arrivalTime("12:05:00")
                .totalDuration("5min")
                .numTransfers(0)
                .connections(List.of(
                        Connection.builder()
                                .tripId("WALK")
                                .departureStopId("A")
                                .arrivalStopId("B")
                                .departureTime("12:00:00")
                                .arrivalTime("12:05:00")
                                .realtimeDelayMinutes(null)
                                .cancelled(null)
                                .build()))
                .hasRealtimeData(false)
                .build();

        Journey result = service.execute(input);

        assertEquals(1, result.getConnections().size());
        Connection walkOut = result.getConnections().get(0);
        assertEquals("WALK", walkOut.getTripId());
    }

    @Test
    void execute_tripPropagaCamposRT() {
        // Bug histórico: el agrupamiento por trip perdía realtimeDelayMinutes/cancelled
        Journey input = Journey.builder()
                .departureTime("12:00:00")
                .arrivalTime("13:00:00")
                .totalDuration("1h")
                .numTransfers(0)
                .connections(List.of(
                        Connection.builder()
                                .tripId("trip_1")
                                .departureStopId("A")
                                .arrivalStopId("B")
                                .departureTime("12:00:00")
                                .arrivalTime("13:00:00")
                                .realtimeDelayMinutes(3)
                                .cancelled(Boolean.FALSE)
                                .build()))
                .hasRealtimeData(true)
                .build();

        Journey result = service.execute(input);

        Connection out = result.getConnections().get(0);
        assertEquals(3, out.getRealtimeDelayMinutes());
        assertEquals(Boolean.FALSE, out.getCancelled());
    }

    @Test
    void execute_agrupaConexionesConsecutivasDelMismoTrip() {
        // Dos conexiones del mismo trip se agrupan en una sola con paradas intermedias
        Journey input = Journey.builder()
                .departureTime("12:00:00")
                .arrivalTime("14:00:00")
                .totalDuration("2h")
                .numTransfers(0)
                .connections(List.of(
                        Connection.builder()
                                .tripId("trip_1")
                                .departureStopId("A")
                                .arrivalStopId("B")
                                .departureTime("12:00:00")
                                .arrivalTime("13:00:00")
                                .build(),
                        Connection.builder()
                                .tripId("trip_1")
                                .departureStopId("B")
                                .arrivalStopId("C")
                                .departureTime("13:00:00")
                                .arrivalTime("14:00:00")
                                .build()))
                .hasRealtimeData(false)
                .build();

        Journey result = service.execute(input);

        // Las dos conexiones del mismo trip se agrupan en una sola
        assertEquals(1, result.getConnections().size());
        Connection grouped = result.getConnections().get(0);
        assertEquals("A", grouped.getDepartureStopId());
        assertEquals("C", grouped.getArrivalStopId());
    }

    @Test
    void execute_dosTripsDistintosNoSeAgrupan() {
        Journey input = Journey.builder()
                .departureTime("12:00:00")
                .arrivalTime("14:00:00")
                .totalDuration("2h")
                .numTransfers(1)
                .connections(List.of(
                        Connection.builder()
                                .tripId("trip_1")
                                .departureStopId("A")
                                .arrivalStopId("B")
                                .departureTime("12:00:00")
                                .arrivalTime("13:00:00")
                                .build(),
                        Connection.builder()
                                .tripId("trip_2")
                                .departureStopId("B")
                                .arrivalStopId("C")
                                .departureTime("13:05:00")
                                .arrivalTime("14:00:00")
                                .build()))
                .hasRealtimeData(false)
                .build();

        Journey result = service.execute(input);

        assertEquals(2, result.getConnections().size());
        assertEquals("trip_1", result.getConnections().get(0).getTripId());
        assertEquals("trip_2", result.getConnections().get(1).getTripId());
    }
}