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

    @GetMapping("/simulation/{simulationId}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID simulationId) {
        EvaluationReport report = service.getRawReportBySimulation(simulationId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + simulationId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report.getPdfContent());
    }
}