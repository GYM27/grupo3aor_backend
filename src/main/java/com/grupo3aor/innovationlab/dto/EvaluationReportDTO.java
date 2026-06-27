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

    // Optional: when provided, the PDF will only include alerts up to this cutoff time
    // This allows PDF generation to proceed independently of the slow bulkDelete in stopSimulation
    private Double cutOffSeconds;

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

    public Double getCutOffSeconds() { return cutOffSeconds; }

    public void setCutOffSeconds(Double cutOffSeconds) { this.cutOffSeconds = cutOffSeconds; }
}