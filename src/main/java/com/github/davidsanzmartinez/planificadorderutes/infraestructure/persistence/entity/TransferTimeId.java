package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferTimeId implements Serializable {

    @Column(name = "from_stop_id")
    private String fromStopId;

    @Column(name = "to_stop_id")
    private String toStopId;

    @Column(name = "from_route_id")
    private String fromRouteId;

    @Column(name = "to_route_id")
    private String toRouteId;
}