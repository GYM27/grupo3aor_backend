package com.grupo3aor.innovationlab.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.dto.RuleCondition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClinicalFormatter {

    private static final ObjectMapper MAPPER = new YAMLMapper();

    public static String formatClinicalMessage(Alert alert) {
        if (alert == null || alert.getValueAtTrigger() == null) {
            return "N/A";
        }

        String dsl = alert.getRule() != null ? alert.getRule().getExpressionDsl() : "";
        Double value = alert.getValueAtTrigger();

        if (dsl == null || dsl.isEmpty()) {
            return formatNumber(value, "") + "";
        }

        try {
            RuleCondition condition = MAPPER.readValue(dsl, RuleCondition.class);
            String metric = condition.getMetric() != null ? condition.getMetric().toUpperCase() : "";
            
            String unit = getUnit(metric);
            String formattedValue = formatValueForMetric(metric, value);

            return (formattedValue + " " + unit).trim();
            
        } catch (Exception e) {
            log.error("Error formatting clinical message: {}", e.getMessage());
            return formatNumber(value, "") + "";
        }
    }

    private static String getUnit(String metric) {
        switch (metric) {
            case "HEART_RATE": return "bpm";
            case "RR": return "cpm";
            case "SPO2": return "%";
            case "BP": return "mmHg";
            case "TIDALVOLUME": return "mL";
            case "ARTERIALBLOODPH": return "";
            case "TEMP": 
            case "TEMPERATURE": return "ºC";
            default: return "";
        }
    }

    private static String formatValueForMetric(String metric, Double value) {
        if (value == null) return "N/A";
        
        switch (metric) {
            case "HEART_RATE":
            case "RR":
            case "TIDALVOLUME":
            case "BP":
                return String.valueOf(Math.round(value));
            case "SPO2":
                // Em BioGears, SpO2 pode vir como 0.95. Se for < 1.0, escala para %.
                double scaledSpO2 = value <= 1.0 ? value * 100 : value;
                return String.valueOf(Math.round(scaledSpO2));
            case "ARTERIALBLOODPH":
            case "TEMP":
            case "TEMPERATURE":
                // 2 casas decimais para pH e Temperatura
                return String.format(java.util.Locale.US, "%.2f", value);
            default:
                return formatNumber(value, metric);
        }
    }

    private static String formatNumber(Double value, String metric) {
        if (value == null) return "N/A";
        if (value == Math.floor(value)) {
            return String.valueOf(value.longValue());
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
