package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "calendar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEntity {

    @Id
    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "monday", nullable = false)
    private boolean monday;

    @Column(name = "tuesday", nullable = false)
    private boolean tuesday;

    @Column(name = "wednesday", nullable = false)
    private boolean wednesday;

    @Column(name = "thursday", nullable = false)
    private boolean thursday;

    @Column(name = "friday", nullable = false)
    private boolean friday;

    @Column(name = "saturday", nullable = false)
    private boolean saturday;

    @Column(name = "sunday", nullable = false)
    private boolean sunday;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
}
