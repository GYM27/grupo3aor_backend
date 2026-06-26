package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.EvaluationReportDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.EvaluationReportRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.dto.RuleCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class EvaluationReportService {

    private final EvaluationReportRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final com.grupo3aor.innovationlab.repository.AlertRepository alertRepository;
    private final com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository readingRepository;

    public EvaluationReportService(EvaluationReportRepository repository, SimulationMapper mapper,
                                   SimulationRepository simulationRepository,
                                   com.grupo3aor.innovationlab.repository.AlertRepository alertRepository,
                                   com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository readingRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
        this.alertRepository = alertRepository;
        this.readingRepository = readingRepository;
    }

    @Transactional
    public EvaluationReportDTO saveReport(EvaluationReportDTO dto, String userEmail, String ipAddress) {
        // I resolve the Simulation entity first to establish a proper FK relation.
        // This ensures reports can only be created for simulations that actually exist.
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + dto.getSimulationId()));

        // Remove any existing report for this simulation to prevent NonUniqueResultException
        repository.findFirstBySimulation_IdOrderByCreatedAtDesc(simulation.getId()).ifPresent(repository::delete);
        repository.flush(); // Ensure deletion is committed before inserting

        EvaluationReport report = mapper.toEntity(dto, simulation);

        report.setCreatedBy(userEmail);
        report.setUpdatedBy(userEmail);
        report.setOriginIp(ipAddress);

        // Generate the PDF content dynamically
        byte[] pdfBytes = generatePdfBytes(report, simulation);
        report.setPdfContent(pdfBytes);

        return mapper.toDto(repository.save(report));
    }

    private byte[] generatePdfBytes(EvaluationReport report, Simulation simulation) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();
            
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            document.add(new Paragraph("VitalSim - Relatorio de Avaliacao", titleFont));
            document.add(new Paragraph("Simulacao: " + simulation.getId(), textFont));
            document.add(new Paragraph("Contexto da Avaliacao: " + (simulation.getScenario() != null ? simulation.getScenario().getName() : "N/A"), textFont));
            // Computar Intervalo Temporal Real
            PhysiologicalReading firstReading = readingRepository.findFirstBySimulation_IdOrderByTimestampAsc(simulation.getId());
            PhysiologicalReading lastReading = readingRepository.findFirstBySimulation_IdOrderByTimestampDesc(simulation.getId());
            String intervaloStr = report.getIntervaloTemporal() != null ? report.getIntervaloTemporal() : "N/A";
            LocalDateTime startBaseTime = simulation.getStartedAt() != null ? simulation.getStartedAt() : LocalDateTime.now();

            if (firstReading != null && firstReading.getTimestamp() != null) {
                startBaseTime = firstReading.getTimestamp();
            }

            document.add(new Paragraph("Data: " + startBaseTime.format(formatter), textFont));

            if (firstReading != null && firstReading.getTimestamp() != null && lastReading != null && lastReading.getTimestamp() != null) {
                LocalDateTime first = firstReading.getTimestamp();
                LocalDateTime last = lastReading.getTimestamp();
                long seconds = Duration.between(first, last).getSeconds();
                long minutes = seconds / 60;
                long secs = seconds % 60;
                intervaloStr = String.format("%02d:%02d minutos de registo", minutes, secs);
            }
            document.add(new Paragraph("Intervalo Temporal: " + intervaloStr, textFont));
            
            document.add(new Paragraph("\n"));
            
            document.add(new Paragraph("Resumo e Justificacao Analitica", subtitleFont));
            document.add(new Paragraph(report.getRationaleText() != null ? report.getRationaleText() : "N/A", textFont));
            document.add(new Paragraph("\n"));

            // 1. Tabela de Alertas (Registo de Eventos)
            document.add(new Paragraph("Registo de Alertas (Eventos Clinicos)", subtitleFont));
            document.add(new Paragraph("\n"));
            
            List<Alert> alerts = alertRepository.findBySimulation_Id(simulation.getId());
            if (alerts == null || alerts.isEmpty()) {
                document.add(new Paragraph("Nenhum alerta registado durante esta simulacao.", textFont));
            } else {
                PdfPTable alertTable = new PdfPTable(5);
                alertTable.setWidthPercentage(100);
                alertTable.setWidths(new float[]{1.5f, 1f, 1.5f, 1f, 3f});
                
                alertTable.addCell(new Phrase("Instante", boldFont));
                alertTable.addCell(new Phrase("Sistema", boldFont));
                alertTable.addCell(new Phrase("Regra", boldFont));
                alertTable.addCell(new Phrase("Alerta", boldFont));
                alertTable.addCell(new Phrase("Rationale Analítico", boldFont));
                
                // Helper class to sort events chronologically
                class ReportEvent implements Comparable<ReportEvent> {
                    LocalDateTime time;
                    String systemName;
                    String ruleName;
                    String severity;
                    String rationale;

                    ReportEvent(LocalDateTime time, String systemName, String ruleName, String severity, String rationale) {
                        this.time = time;
                        this.systemName = systemName;
                        this.ruleName = ruleName;
                        this.severity = severity;
                        this.rationale = rationale;
                    }

                    @Override
                    public int compareTo(ReportEvent o) {
                        if (this.time == null && o.time == null) return 0;
                        if (this.time == null) return -1;
                        if (o.time == null) return 1;
                        return this.time.compareTo(o.time);
                    }
                }

                List<ReportEvent> events = new ArrayList<>();
                for (Alert a : alerts) {
                    String sysName = a.getRule() != null && a.getRule().getSystem() != null ? a.getRule().getSystem().getSystemName() : "N/A";
                    String rName = a.getRule() != null ? a.getRule().getName() : "N/A";
                    String triggerSev = a.getRule() != null ? a.getRule().getSeverity().name() : "N/A";
                    String justif = generateAnalyticalRationale(a, false);

                    if (a.getTimestamp() != null) {
                        events.add(new ReportEvent(a.getTimestamp(), sysName, rName, triggerSev, justif));
                    }
                    if (a.getWarningAt() != null) {
                        events.add(new ReportEvent(a.getWarningAt(), sysName, rName, "AVISO (Melhoria)", "Os sinais vitais começam a regressar à zona de segurança."));
                    }
                    if (a.getResolvedAt() != null) {
                        events.add(new ReportEvent(a.getResolvedAt(), sysName, rName + " (Estabilizado)", "NORMAL", "Paciente estabilizado. Valores normalizados."));
                    }
                }
                
                Collections.sort(events);

                for (ReportEvent ev : events) {
                    String instante = "N/A";
                    LocalDateTime startBase = (firstReading != null && firstReading.getTimestamp() != null) 
                            ? firstReading.getTimestamp() 
                            : (simulation.getStartedAt() != null ? simulation.getStartedAt() : null);
                    if (ev.time != null && startBase != null) {
                        long diffSecs = Duration.between(startBase, ev.time).getSeconds();
                        diffSecs = Math.max(0, diffSecs);
                        instante = String.format("%02d:%02d", diffSecs / 60, diffSecs % 60);
                    } else if (ev.time != null) {
                        instante = ev.time.format(timeFormatter);
                    }

                    alertTable.addCell(new Phrase(instante != null ? instante : "N/A", textFont));
                    alertTable.addCell(new Phrase(ev.systemName != null ? ev.systemName : "N/A", textFont));
                    alertTable.addCell(new Phrase(ev.ruleName != null ? ev.ruleName : "N/A", textFont));
                    alertTable.addCell(new Phrase(ev.severity != null ? ev.severity : "N/A", textFont));
                    alertTable.addCell(new Phrase(ev.rationale != null ? ev.rationale : "N/A", textFont));
                }
                document.add(alertTable);
            }
            
            // Fechar o documento finaliza a escrita no ByteArrayOutputStream
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o documento PDF do Relatorio", e);
        }
    }

    private String generateAnalyticalRationale(Alert alert, boolean isResolved) {
        if (alert.getRule() == null) {
            return "Ativação baseada nas regras configuradas.";
        }
        if (alert.getRule().getAnalyticalJustification() != null && !alert.getRule().getAnalyticalJustification().trim().isEmpty()) {
            return alert.getRule().getAnalyticalJustification();
        }
        if (alert.getRule().getExpressionDsl() == null) {
            return "Ativação baseada nas regras configuradas.";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            RuleCondition condition = mapper.readValue(alert.getRule().getExpressionDsl(), RuleCondition.class);
            
            String verb = "violou a condição";
            if ("<".equals(condition.getOperator())) verb = "ficou abaixo do limite";
            else if (">".equals(condition.getOperator())) verb = "superou o limite";
            else if ("==".equals(condition.getOperator())) verb = "igualou o valor crítico";

            return String.format("A métrica '%s' registou o valor %.2f, o que %s definido (%s %.2f).",
                    condition.getMetric(),
                    alert.getValueAtTrigger(),
                    verb,
                    condition.getOperator(),
                    condition.getActivationThreshold() != null ? condition.getActivationThreshold() : 0.0);
        } catch (Exception e) {
            return String.format("O valor %.2f disparou a condição analítica: %s", alert.getValueAtTrigger(), alert.getRule().getExpressionDsl());
        }
    }

    @Transactional(readOnly = true)
    public EvaluationReport getRawReportBySimulation(UUID simulationId) {
        return repository.findFirstBySimulation_IdOrderByCreatedAtDesc(simulationId)
                .orElseThrow(() -> new RuntimeException("Evaluation report missing for target simulation context"));
    }
}
