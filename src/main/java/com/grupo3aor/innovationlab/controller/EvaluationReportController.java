package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.dto.EvaluationReportDTO;
import com.grupo3aor.innovationlab.service.EvaluationReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * REST controller for managing evaluation reports.
 * Provides endpoints for creating reports, fetching reports by simulation, and downloading reports as PDFs.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("isAuthenticated()")
public class EvaluationReportController {

    private final EvaluationReportService service;

    public EvaluationReportController(EvaluationReportService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EvaluationReportDTO> createReport(
            @Valid @RequestBody EvaluationReportDTO dto,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.saveReport(dto, authentication.getName(), request.getRemoteAddr()));
    }

    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<java.util.List<EvaluationReportDTO>> getReportsBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getAllReportsBySimulation(simulationId));
    }

    @GetMapping("/download/{reportId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID reportId) {
        EvaluationReport report = service.getRawReportById(reportId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + reportId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report.getPdfContent());
    }

    /**
     * Combined endpoint: generates the PDF report and returns it in one HTTP call.
     * Eliminates the need for a separate POST + GET sequence.
     */
    @PostMapping("/generate-and-download")
    public ResponseEntity<byte[]> generateAndDownload(
            @Valid @RequestBody EvaluationReportDTO dto,
            Authentication authentication,
            HttpServletRequest request) {
        byte[] pdfBytes = service.generateAndDownloadPdf(dto, authentication.getName(), request.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + dto.getSimulationId() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}