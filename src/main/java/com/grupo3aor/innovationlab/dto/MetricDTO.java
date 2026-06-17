package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para mapear as leituras individuais de métricas que vêm dentro do ficheiro JSON
 * do cenário clínico. Cada MetricDTO representa um ponto no tempo com um valor fisiológico.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricDTO {
    private String handle;
    private String unit;
    private BigDecimal value;
    private String timestamp; // Formato esperado: "2026-03-17T10:00:05Z"
}
