package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity enclosing textual and downloadable binary evaluations of executed simulations.
 *
 */
@Entity
@Table(name = "EVALUATION_REPORT")
public class EvaluationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "simulation_id", nullable = false)
    private UUID simulationId;

    @Column(name = "intervalo_temporal", nullable = false)
    private String intervaloTemporal;

    @Lob
    @Column(name = "rationale_text", nullable = false, columnDefinition = "TEXT")
    private String rationaleText;

    @Lob
    @Column(name = "pdf_content", nullable = false)
    private byte[] pdfContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy; // Changed to Long to match group implementation

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy; // Changed to Long to match group implementation

    @Column(name = "origin_ip", nullable = false)
    private String originIp;

    public EvaluationReport() {
    }

    // --- Getters and Setters ---
    public UUID getId() { return id; }

    public void setId(UUID id) { this.id = id; }

    public UUID getSimulationId() { return simulationId; }

    public void setSimulationId(UUID simulationId) { this.simulationId = simulationId; }

    public String getIntervaloTemporal() { return intervaloTemporal; }

    public void setIntervaloTemporal(String intervaloTemporal) { this.intervaloTemporal = intervaloTemporal; }

    public String getRationaleText() { return rationaleText; }

    public void setRationaleText(String rationaleText) { this.rationaleText = rationaleText; }

    public byte[] getPdfContent() { return pdfContent; }

    public void setPdfContent(byte[] pdfContent) { this.pdfContent = pdfContent; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatedBy() { return createdBy; }

    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }

    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public String getOriginIp() { return originIp; }

    public void setOriginIp(String originIp) { this.originIp = originIp; }
}