package com.grupo3aor.innovationlab.dto;

import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Inbound payload representing an administrative request to update a user's role.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserRequest {
    
    @NotNull(message = "Profile role cannot be null")
    private PerfilEnum perfil;

}
