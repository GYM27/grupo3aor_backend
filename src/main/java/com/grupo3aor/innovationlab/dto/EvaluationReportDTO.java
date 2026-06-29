package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Data Transfer Contract mapping summary data and downloadable report byte arrays.
 */
public class EvaluationReportDTO {

    private UUID id;

    @NotNull(message = "Simulation ID is mandatory")
    private UUID simulationId;

    @NotBlank(message = "Temporal interval metadata cannot be blank")
    private String intervaloTemporal;

    @NotBlank(message = "Behavioral explanation text is required")
    private String rationaleText;

    private byte[] pdfContent;

    // Time limits for the 'Relatório de Sessão'
    private Double startObservation;
    private Double endObservation;

    // Flag to determine if the report is for a Live stream (use Local Time) or Batch (use Duration)
    private Boolean isLive;
    
    // Audit fields to return to the UI
    private String createdBy;
    private java.time.LocalDateTime createdAt;

    public EvaluationReportDTO() {
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

    public Double getStartObservation() { return startObservation; }

    public void setStartObservation(Double startObservation) { this.startObservation = startObservation; }

    public Double getEndObservation() { return endObservation; }

    public void setEndObservation(Double endObservation) { this.endObservation = endObservation; }

    public Boolean getIsLive() { return isLive; }

    public void setIsLive(Boolean isLive) { this.isLive = isLive; }
    
    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}