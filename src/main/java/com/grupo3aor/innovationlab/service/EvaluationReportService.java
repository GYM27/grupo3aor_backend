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
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.domain.enums.Severity;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;

@Slf4j
@Service
/**
 * Service responsible for generating and managing evaluation reports.
 */
public class EvaluationReportService {

    private final EvaluationReportRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final AlertRepository alertRepository;
    private final PhysiologicalReadingRepository readingRepository;
    private final ClinicalFormatter clinicalFormatter;
    private final RuleRepository ruleRepository;

    public EvaluationReportService(EvaluationReportRepository repository, SimulationMapper mapper,
                                   SimulationRepository simulationRepository,
                                   AlertRepository alertRepository,
                                   PhysiologicalReadingRepository readingRepository,
                                   ClinicalFormatter clinicalFormatter,
                                   RuleRepository ruleRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
        this.alertRepository = alertRepository;
        this.readingRepository = readingRepository;
        this.clinicalFormatter = clinicalFormatter;
        this.ruleRepository = ruleRepository;
    }

    @Transactional
    public EvaluationReportDTO saveReport(EvaluationReportDTO dto, String userEmail, String ipAddress) {
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + dto.getSimulationId()));

        // I removed the delete logic here to allow multiple reports per simulation (Session Report)
        
        EvaluationReport report = mapper.toEntity(dto, simulation);

        report.setCreatedBy(userEmail);
        report.setUpdatedBy(userEmail);
        report.setOriginIp(ipAddress);

        byte[] pdfBytes = generatePdfBytes(report, simulation, dto.getStartObservation(), dto.getEndObservation(), dto.getIsLive());
        report.setPdfContent(pdfBytes);

        return mapper.toDto(repository.save(report));
    }

    private byte[] generatePdfBytes(EvaluationReport report, Simulation simulation, Double startObservation, Double endObservation, Boolean isLive) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();
            
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font disclaimerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            document.add(new Paragraph("VitalSim - Relatório Técnico de Avaliação", titleFont));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("CABEÇALHO FACTUAL", subtitleFont));
            document.add(new Paragraph("ID da Simulação: " + simulation.getId(), textFont));
            document.add(new Paragraph("Contexto / Paciente: " + (simulation.getScenario() != null ? simulation.getScenario().getName() : "N/A"), textFont));
            document.add(new Paragraph("Operador: " + report.getCreatedBy(), textFont));
            
            // Rules in execution
            String rulesNames = "N/A";
            List<Rule> activeRules = ruleRepository.findByActiveTrue();
            if (activeRules != null && !activeRules.isEmpty()) {
                rulesNames = activeRules.stream().map(Rule::getName).collect(Collectors.joining(", "));
            }
            document.add(new Paragraph("Regras em Execução: " + rulesNames, textFont));
            
            String intervaloStr = report.getIntervaloTemporal() != null ? report.getIntervaloTemporal() : "N/A";
            LocalDateTime startBaseTime = simulation.getStartedAt() != null ? simulation.getStartedAt() : LocalDateTime.now();

            document.add(new Paragraph("Data de Início Original: " + startBaseTime.format(formatter), textFont));
            document.add(new Paragraph("Intervalo de Observação: " + intervaloStr, textFont));
            document.add(new Paragraph("\n"));
            
            List<Alert> alerts;
            LocalDateTime startCutOff = null;
            LocalDateTime endCutOff = null;
            
            if (simulation.getStartedAt() != null) {
                if (startObservation != null) {
                    startCutOff = simulation.getStartedAt().plusNanos((long)(startObservation * 1_000_000_000L));
                }
                if (endObservation != null) {
                    endCutOff = simulation.getStartedAt().plusNanos((long)(endObservation * 1_000_000_000L)).plusSeconds(1);
                }
            }
            
            if (endCutOff != null) {
                alerts = alertRepository.findBySimulationIdUpToCutoff(simulation.getId(), endCutOff);
                log.info("PDF generation: filtered alerts up to {} for simulation {}", endCutOff, simulation.getId());
            } else {
                alerts = alertRepository.findBySimulation_Id(simulation.getId());
            }

            document.add(new Paragraph("Registo Cronológico de Eventos Fisiológicos", subtitleFont));
            document.add(new Paragraph("\n"));
            
            if (alerts == null || alerts.isEmpty()) {
                document.add(new Paragraph("Nenhum evento registado durante esta simulação.", textFont));
            } else {
                PdfPTable alertTable = new PdfPTable(5);
                alertTable.setWidthPercentage(100);
                alertTable.setWidths(new float[]{0.6f, 1.5f, 1.5f, 1.2f, 3f});
                
                alertTable.addCell(new Phrase("Instante", boldFont));
                alertTable.addCell(new Phrase("Sistema", boldFont));
                alertTable.addCell(new Phrase("Regra", boldFont));
                alertTable.addCell(new Phrase("Tipo Evento", boldFont));
                alertTable.addCell(new Phrase("Racional Legível", boldFont));
                
                class ReportEvent implements Comparable<ReportEvent> {
                    LocalDateTime time;
                    String systemName;
                    String ruleName;
                    String type;
                    String rationale;
                    Color bgColor;

                    ReportEvent(LocalDateTime time, String systemName, String ruleName, String type, String rationale, Color bgColor) {
                        this.time = time;
                        this.systemName = systemName;
                        this.ruleName = ruleName;
                        this.type = type;
                        this.rationale = rationale;
                        this.bgColor = bgColor;
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

                // Pastel colors
                Color redPastel = new Color(255, 180, 171, 100);
                Color yellowPastel = new Color(250, 189, 0, 100);
                Color greenPastel = new Color(129, 201, 149, 100);
                Color orangePastel = new Color(255, 213, 79, 100);

                for (Alert a : alerts) {
                    String rName = (a.getRule() != null && a.getRule().getName() != null && !a.getRule().getName().isEmpty())
                                   ? a.getRule().getName()
                                   : "Regra Desconhecida";
                    
                    String sysName = (a.getRule() != null && a.getRule().getSystem() != null)
                                     ? a.getRule().getSystem().getSystemName()
                                     : "N/A";

                    String triggerSev = (a.getRule() != null && a.getRule().getSeverity() != null) 
                                        ? a.getRule().getSeverity().name() 
                                        : "WARNING";

                    Color triggerColor = "CRITICO".equalsIgnoreCase(triggerSev) ? redPastel : orangePastel;
                    String justif = clinicalFormatter.formatRationale(a);

                    if (a.getTimestamp() != null && (endCutOff == null || !a.getTimestamp().isAfter(endCutOff)) && (startCutOff == null || !a.getTimestamp().isBefore(startCutOff))) {
                        events.add(new ReportEvent(a.getTimestamp(), sysName, rName, triggerSev.toUpperCase(), justif, triggerColor));
                    }
                    if (a.getWarningAt() != null && (endCutOff == null || !a.getWarningAt().isAfter(endCutOff)) && (startCutOff == null || !a.getWarningAt().isBefore(startCutOff))) {
                        events.add(new ReportEvent(a.getWarningAt(), sysName, rName, "AVISO", "Valores iniciam aproximação à zona limite.", yellowPastel));
                    }
                    if (a.getResolvedAt() != null && (endCutOff == null || !a.getResolvedAt().isAfter(endCutOff)) && (startCutOff == null || !a.getResolvedAt().isBefore(startCutOff))) {
                        events.add(new ReportEvent(a.getResolvedAt(), sysName, rName, "NORMALIZAÇÃO", "Parâmetro dentro dos limites predefinidos.", greenPastel));
                    }
                }
                
                Collections.sort(events);

                for (ReportEvent ev : events) {
                    String instante = "N/A";
                    if (Boolean.TRUE.equals(isLive)) {
                        if (ev.time != null) {
                            instante = ev.time.format(timeFormatter);
                        }
                    } else {
                        if (ev.time != null && startBaseTime != null) {
                            long diffSecs = Duration.between(startBaseTime, ev.time).getSeconds();
                            diffSecs = Math.max(0, diffSecs);
                            instante = String.format("%02d:%02d", diffSecs / 60, diffSecs % 60);
                        } else if (ev.time != null) {
                            instante = ev.time.format(timeFormatter);
                        }
                    }

                    PdfPCell cell1 = new PdfPCell(new Phrase(instante != null ? instante : "N/A", textFont));
                    PdfPCell cell2 = new PdfPCell(new Phrase(ev.systemName != null ? ev.systemName : "N/A", textFont));
                    PdfPCell cell3 = new PdfPCell(new Phrase(ev.ruleName != null ? ev.ruleName : "N/A", textFont));
                    PdfPCell cell4 = new PdfPCell(new Phrase(ev.type != null ? ev.type : "N/A", textFont));
                    PdfPCell cell5 = new PdfPCell(new Phrase(ev.rationale != null ? ev.rationale : "N/A", textFont));

                    if (ev.bgColor != null) {
                        cell1.setBackgroundColor(ev.bgColor);
                        cell2.setBackgroundColor(ev.bgColor);
                        cell3.setBackgroundColor(ev.bgColor);
                        cell4.setBackgroundColor(ev.bgColor);
                        cell5.setBackgroundColor(ev.bgColor);
                    }

                    alertTable.addCell(cell1);
                    alertTable.addCell(cell2);
                    alertTable.addCell(cell3);
                    alertTable.addCell(cell4);
                    alertTable.addCell(cell5);
                }
                document.add(alertTable);
            }
            
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Nota: Este relatório descreve eventos baseados em regras predefinidas e não constitui diagnóstico clínico.", disclaimerFont));
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o documento PDF do Relatorio", e);
        }
    }

    @Transactional(readOnly = true)
    public List<EvaluationReportDTO> getAllReportsBySimulation(UUID simulationId) {
        return repository.findBySimulation_IdOrderByCreatedAtDesc(simulationId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvaluationReport getRawReportById(UUID reportId) {
        return repository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Evaluation report not found"));
    }

    /**
     * Combined save + download in one transactional call.
     * Eliminates the second HTTP roundtrip for PDF download.
     */
    @Transactional
    public byte[] generateAndDownloadPdf(EvaluationReportDTO dto, String userEmail, String ipAddress) {
        EvaluationReportDTO saved = saveReport(dto, userEmail, ipAddress);
        return saved.getPdfContent();
    }
}
