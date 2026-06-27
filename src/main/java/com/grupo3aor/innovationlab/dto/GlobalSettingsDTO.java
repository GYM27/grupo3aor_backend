package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalSettingsDTO {
    private Integer sessionTimeoutMinutes;
    private Boolean isHumanBodyEnabled;
}
