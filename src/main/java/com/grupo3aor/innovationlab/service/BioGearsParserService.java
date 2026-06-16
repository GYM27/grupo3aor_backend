package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BioGearsParserService {

    public List<PhysiologicalReadingDTO> parseCsv(MultipartFile file, UUID simulationId) throws Exception {
        List<PhysiologicalReadingDTO> readings = new ArrayList<>();
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {
             
            String[] header = csvReader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            // Mapear dinamicamente as colunas do BioGears
            int timeIdx = -1, hrIdx = -1, spo2Idx = -1, sbpIdx = -1, tempIdx = -1;
            for (int i = 0; i < header.length; i++) {
                String col = header[i].trim();
                if (col.startsWith("Time")) timeIdx = i;
                else if (col.startsWith("HeartRate")) hrIdx = i;
                else if (col.startsWith("OxygenSaturation")) spo2Idx = i;
                else if (col.startsWith("SystolicArterialPressure")) sbpIdx = i;
                else if (col.startsWith("CoreTemperature")) tempIdx = i;
            }
            
            if (timeIdx == -1) throw new IllegalArgumentException("CSV missing 'Time(s)' column");

            String[] row;
            LocalDateTime baseTime = LocalDateTime.now(); // Ancora de tempo para o "Playback"
            
            while ((row = csvReader.readNext()) != null) {
                if (row.length <= timeIdx || row[timeIdx].isEmpty()) continue;
                
                double timeSeconds = Double.parseDouble(row[timeIdx]);
                // Para o histórico fluir corretamente, somamos os segundos ao instante atual
                LocalDateTime timestamp = baseTime.plusNanos((long) (timeSeconds * 1_000_000_000L));

                // Parsing e geração do DTO de Heart Rate
                if (hrIdx != -1 && hrIdx < row.length && !row[hrIdx].isEmpty()) {
                    readings.add(createDTO(simulationId, "HR", "bpm", row[hrIdx], timestamp, 1.0));
                }
                
                // Parsing de SPO2 (BioGears dá 0.98, multiplicamos por 100 para ter 98%)
                if (spo2Idx != -1 && spo2Idx < row.length && !row[spo2Idx].isEmpty()) {
                    readings.add(createDTO(simulationId, "SPO2", "%", row[spo2Idx], timestamp, 100.0));
                }
                
                // Parsing de Systolic Blood Pressure
                if (sbpIdx != -1 && sbpIdx < row.length && !row[sbpIdx].isEmpty()) {
                    readings.add(createDTO(simulationId, "SBP", "mmHg", row[sbpIdx], timestamp, 1.0));
                }

                // Parsing de Temperature
                if (tempIdx != -1 && tempIdx < row.length && !row[tempIdx].isEmpty()) {
                    readings.add(createDTO(simulationId, "TEMP", "C", row[tempIdx], timestamp, 1.0));
                }
            }
        }
        
        return readings;
    }
    
    private PhysiologicalReadingDTO createDTO(UUID simulationId, String handle, String unit, String rawValue, LocalDateTime timestamp, double multiplier) {
        PhysiologicalReadingDTO dto = new PhysiologicalReadingDTO();
        dto.setSimulationId(simulationId);
        dto.setHandle(handle);
        dto.setUnit(unit);
        dto.setTimestamp(timestamp);
        
        BigDecimal value = new BigDecimal(rawValue).multiply(new BigDecimal(multiplier));
        // Arredondamos a 2 casas decimais para manter os gráficos limpos
        dto.setValue(value.setScale(2, RoundingMode.HALF_UP));
        return dto;
    }
}
