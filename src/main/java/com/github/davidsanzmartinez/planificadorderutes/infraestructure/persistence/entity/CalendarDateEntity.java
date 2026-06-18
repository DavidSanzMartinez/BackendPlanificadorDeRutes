package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendar_dates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDateEntity {

    @EmbeddedId
    private CalendarDateId id;

    @Column(name = "exception_type", nullable = false)
    private int exceptionType;
}
