package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for global settings.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalSettingsDTO {
    private Integer sessionTimeoutMinutes;
    private Boolean isHumanBodyEnabled;
}
