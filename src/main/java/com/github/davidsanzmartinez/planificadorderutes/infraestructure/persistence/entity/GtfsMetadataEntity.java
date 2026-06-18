package com.github.davidsanzmartinez.planificadorderutes.infraestructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "gtfs_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GtfsMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, unique = true)
    private String fileName;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "loaded_at", nullable = false)
    private LocalDateTime loadedAt;
}
