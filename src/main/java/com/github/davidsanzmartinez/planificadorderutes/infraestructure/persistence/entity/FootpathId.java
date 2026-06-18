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
public class FootpathId implements Serializable {

    @Column(name = "from_stop_id")
    private String fromStopId;

    @Column(name = "to_stop_id")
    private String toStopId;
}