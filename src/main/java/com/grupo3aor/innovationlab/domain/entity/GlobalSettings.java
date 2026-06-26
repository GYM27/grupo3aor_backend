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
    private Long id ; // Forces the ID to always be 1 in the database

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes; // Defines the session inactivity timeout limit

    @Column(name = "human_body_enabled")
    private Boolean isHumanBodyEnabled; // Toggles the 3D anatomy model visibility on the dashboard.

    @jakarta.persistence.Transient
    @lombok.Builder.Default
    private Boolean isDbFailed = false; // Chaos Engineering flag. It stays strictly in memory to simulate database crashes, avoiding persistent chaos.
}