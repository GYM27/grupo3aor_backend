package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.EvaluationReportDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.EvaluationReportRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();
            
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font subtitleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font textFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10);
            com.lowagie.text.Font boldFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10);
            
            document.add(new com.lowagie.text.Paragraph("VitalSim - Relatorio de Avaliacao", titleFont));
            document.add(new com.lowagie.text.Paragraph("Simulacao: " + simulation.getId(), textFont));
            document.add(new com.lowagie.text.Paragraph("Data: " + java.time.LocalDateTime.now(), textFont));
            document.add(new com.lowagie.text.Paragraph("Intervalo Temporal: " + report.getIntervaloTemporal(), textFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            document.add(new com.lowagie.text.Paragraph("Resumo e Justificacao Analitica", subtitleFont));
            document.add(new com.lowagie.text.Paragraph(report.getRationaleText(), textFont));
            document.add(new com.lowagie.text.Paragraph("\n"));

            // 1. Tabela de Alertas (Registo de Eventos)
            document.add(new com.lowagie.text.Paragraph("Registo de Alertas (Eventos Clinicos)", subtitleFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            java.util.List<com.grupo3aor.innovationlab.domain.entity.Alert> alerts = alertRepository.findBySimulation_Id(simulation.getId());
            if (alerts == null || alerts.isEmpty()) {
                document.add(new com.lowagie.text.Paragraph("Nenhum alerta registado durante esta simulacao.", textFont));
            } else {
                com.lowagie.text.pdf.PdfPTable alertTable = new com.lowagie.text.pdf.PdfPTable(3);
                alertTable.setWidthPercentage(100);
                alertTable.setWidths(new float[]{1.5f, 1f, 3.5f});
                
                alertTable.addCell(new com.lowagie.text.Phrase("Data/Hora", boldFont));
                alertTable.addCell(new com.lowagie.text.Phrase("Severidade", boldFont));
                alertTable.addCell(new com.lowagie.text.Phrase("Mensagem", boldFont));
                
                for (com.grupo3aor.innovationlab.domain.entity.Alert a : alerts) {
                    alertTable.addCell(new com.lowagie.text.Phrase(a.getCreatedAt().toString(), textFont));
                    alertTable.addCell(new com.lowagie.text.Phrase(a.getRule().getSeverity().name(), textFont));
                    alertTable.addCell(new com.lowagie.text.Phrase(a.getRule().getExpressionDsl(), textFont));
                }
                document.add(alertTable);
            }
            
            document.add(new com.lowagie.text.Paragraph("\n"));

            // 2. Tabela de Leituras Fisiologicas (Resumo / Amostra)
            document.add(new com.lowagie.text.Paragraph("Resumo de Leituras Fisiologicas", subtitleFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            java.util.List<com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading> readings = readingRepository.findBySimulation_Id(simulation.getId());
            if (readings == null || readings.isEmpty()) {
                document.add(new com.lowagie.text.Paragraph("Nenhuma leitura captada.", textFont));
            } else {
                com.lowagie.text.pdf.PdfPTable readingTable = new com.lowagie.text.pdf.PdfPTable(3);
                readingTable.setWidthPercentage(100);
                readingTable.setWidths(new float[]{2f, 2f, 1.5f});
                
                readingTable.addCell(new com.lowagie.text.Phrase("Sinal Vital", boldFont));
                readingTable.addCell(new com.lowagie.text.Phrase("Valor", boldFont));
                readingTable.addCell(new com.lowagie.text.Phrase("Data/Hora", boldFont));
                
                // Limitar para não gerar um PDF de mil paginas (mostramos so as ultimas 20 leituras)
                int limit = Math.min(readings.size(), 20);
                for (int i = readings.size() - limit; i < readings.size(); i++) {
                    com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading r = readings.get(i);
                    readingTable.addCell(new com.lowagie.text.Phrase(r.getHandle(), textFont));
                    readingTable.addCell(new com.lowagie.text.Phrase(r.getValue() + " " + r.getUnit(), textFont));
                    readingTable.addCell(new com.lowagie.text.Phrase(r.getTimestamp().toString(), textFont));
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

    @Transactional(readOnly = true)
    public EvaluationReport getRawReportBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId)
                .orElseThrow(() -> new RuntimeException("Evaluation report missing for target simulation context"));
    }
}