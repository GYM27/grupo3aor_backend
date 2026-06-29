package com.grupo3aor.innovationlab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.dto.RuleCondition;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service responsible for formatting clinical rationales based on strict factual neutrality.
 * No clinical inferences, diagnostics or value judgements are made here.
 */
@Service
public class ClinicalFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final java.util.concurrent.ConcurrentHashMap<java.util.UUID, RuleCondition> conditionCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Formats a strictly factual rationale string.
     * Output format: '[Parameter] was [above/below] the defined threshold ([Threshold Value]). Observed value: [Observed Value].'
     *
     * @param alert The triggered alert
     * @return Formatted analytical rationale
     */
    public String formatRationale(Alert alert) {
        if (alert == null || alert.getRule() == null) {
            return "Activation based on configured rules.";
        }

        RuleCondition condition = null;
        try {
            if (alert.getRule().getId() != null && alert.getRule().getExpressionDsl() != null) {
                condition = conditionCache.computeIfAbsent(alert.getRule().getId(), k -> {
                    try {
                        return MAPPER.readValue(alert.getRule().getExpressionDsl(), RuleCondition.class);
                    } catch (Exception e) {
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            // Ignored, condition remains null
        }

        if (condition != null) {
            String verb = "violated the condition";
            if ("<".equals(condition.getOperator()) || "<=".equals(condition.getOperator())) verb = "below";
            else if (">".equals(condition.getOperator()) || ">=".equals(condition.getOperator())) verb = "above";
            else if ("==".equals(condition.getOperator())) verb = "equal to";
            
            double threshold = condition.getActivationThreshold() != null ? condition.getActivationThreshold() : 0.0;
            double observed = alert.getValueAtTrigger() != null ? alert.getValueAtTrigger() : 0.0;
            
            String unit = getUnitForMetric(condition.getMetric());
            
            // Format strictly to: '[Parameter] was [above/below] the defined threshold ([Threshold Value]). Observed value: [Observed Value].'
            return String.format(Locale.US, "The parameter %s was %s the defined threshold (%.1f%s). Observed value: %.1f%s.",
                    condition.getMetric(), verb, threshold, unit, observed, unit);
        }

        return "Activation based on configured rules. Observed value: " + String.format(Locale.US, "%.1f", alert.getValueAtTrigger());
    }
    
    private String getUnitForMetric(String metric) {
        if (metric == null) return "";
        metric = metric.toUpperCase();
        if (metric.contains("SPO2") || metric.contains("OXYGEN")) return "%";
        if (metric.contains("HR") || metric.contains("HEART") || metric.contains("RATE")) return " bpm";
        if (metric.contains("PRESSURE") || metric.contains("BP")) return " mmHg";
        if (metric.contains("TEMP")) return " °C";
        return "";
    }
}
