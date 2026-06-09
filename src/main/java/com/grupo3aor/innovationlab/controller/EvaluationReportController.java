package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.EvaluationReportDTO;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.EvaluationReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class EvaluationReportController {

    private final EvaluationReportService service;
    private final UserRepository userRepository;

    public EvaluationReportController(EvaluationReportService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<EvaluationReportDTO> createReport(
            @Valid @RequestBody EvaluationReportDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.saveReport(dto, getCurrentAuthenticatedUser(), request.getRemoteAddr()));
    }

    @GetMapping("/simulation/{simulationId}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID simulationId) {
        EvaluationReport report = service.getRawReportBySimulation(simulationId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + simulationId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report.getPdfContent());
    }

    private Long getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: Current session context missing authenticated principal");
        }
        String sessionEmail = authentication.getName();
        return userRepository.findByEmail(sessionEmail)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Entity profile error for active user: " + sessionEmail));
    }
}