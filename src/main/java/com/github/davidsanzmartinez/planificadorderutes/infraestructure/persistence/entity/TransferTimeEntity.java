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
@Table(name = "transfer_times")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTimeEntity {

    @EmbeddedId
    private TransferTimeId id;

    @Column(name = "transfer_type", nullable = false)
    private int transferType;

    @Column(name = "min_transfer_time", nullable = false)
    private int minTransferTime;
}