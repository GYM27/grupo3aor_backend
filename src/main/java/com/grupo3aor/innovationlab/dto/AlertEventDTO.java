package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Lightweight representation of an Alert specifically for the History Timeline.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertEventDTO {
    private LocalDateTime timestamp;
    private String description;
    private String type; // e.g., 'critical', 'warning', 'info'
}
