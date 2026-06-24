package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "global_settings")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalSettings extends Auditable {

    @Id
    private Long id ; // Força a ser sempre o ID 1 na BD

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes; // Mapeia o Limite de Inatividade

    @Column(name = "human_body_enabled")
    private Boolean isHumanBodyEnabled; // Mapeia a visualização do corpo humano na dashboard

}