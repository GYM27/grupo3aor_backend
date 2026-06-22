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
            document.add(new Paragraph("Data: " + LocalDateTime.now().format(formatter), textFont));
            
            // Computar Intervalo Temporal Real
            List<PhysiologicalReading> readings = readingRepository.findBySimulation_IdOrderByTimestampAsc(simulation.getId());
            String intervaloStr = report.getIntervaloTemporal();
            if (readings != null && !readings.isEmpty()) {
                LocalDateTime first = readings.get(0).getTimestamp();
                LocalDateTime last = readings.get(readings.size() - 1).getTimestamp();
                long seconds = Duration.between(first, last).getSeconds();
                long minutes = seconds / 60;
                long secs = seconds % 60;
                intervaloStr = String.format("%02d:%02d minutos de registo", minutes, secs);
            }
            document.add(new Paragraph("Intervalo Temporal: " + intervaloStr, textFont));
            
            document.add(new Paragraph("\n"));
            
            document.add(new Paragraph("Resumo e Justificacao Analitica", subtitleFont));
            document.add(new Paragraph(report.getRationaleText(), textFont));
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
                
                for (Alert a : alerts) {
                    alertTable.addCell(new Phrase(a.getCreatedAt() != null ? a.getCreatedAt().format(timeFormatter) : "N/A", textFont));
                    alertTable.addCell(new Phrase(a.getRule() != null && a.getRule().getSystem() != null ? a.getRule().getSystem().getSystemName() : "N/A", textFont));
                    alertTable.addCell(new Phrase(a.getRule() != null ? a.getRule().getName() : "N/A", textFont));
                    alertTable.addCell(new Phrase(a.getRule() != null ? a.getRule().getSeverity().name() : "N/A", textFont));
                    alertTable.addCell(new Phrase(generateAnalyticalRationale(a), textFont));
                }
                document.add(alertTable);
            }
            
            document.add(new com.lowagie.text.Paragraph("\n"));

            // 2. Tabela de Leituras Fisiologicas (Resumo / Amostra)
            document.add(new Paragraph("Resumo de Leituras Fisiologicas", subtitleFont));
            document.add(new Paragraph("\n"));
            
            if (readings == null || readings.isEmpty()) {
                document.add(new Paragraph("Nenhuma leitura captada.", textFont));
            } else {
                PdfPTable readingTable = new PdfPTable(3);
                readingTable.setWidthPercentage(100);
                readingTable.setWidths(new float[]{2f, 2f, 1.5f});
                
                readingTable.addCell(new Phrase("Sinal Vital", boldFont));
                readingTable.addCell(new Phrase("Valor", boldFont));
                readingTable.addCell(new Phrase("Hora", boldFont));
                
                // Limitar para não gerar um PDF de mil paginas (mostramos so as ultimas 20 leituras)
                int limit = Math.min(readings.size(), 20);
                for (int i = readings.size() - limit; i < readings.size(); i++) {
                    PhysiologicalReading r = readings.get(i);
                    readingTable.addCell(new Phrase(r.getHandle(), textFont));
                    readingTable.addCell(new Phrase(r.getValue() + " " + r.getUnit(), textFont));
                    readingTable.addCell(new Phrase(r.getTimestamp().format(timeFormatter), textFont));
                }
                document.add(readingTable);
            }
            
            // Fechar o documento finaliza a escrita no ByteArrayOutputStream
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o documento PDF do Relatorio", e);
        }
    }

    private String generateAnalyticalRationale(Alert alert) {
        if (alert.getRule() == null || alert.getRule().getExpressionDsl() == null) {
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
                    condition.getThreshold());
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
