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


/**
 * JPA entity representing the global application settings.
 * Ensures only a single configuration record exists in the system.
 */
@Entity
@Table(name = "global_settings")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalSettings extends Auditable {

    @Id
    private Long id ; // Forces it to always be ID 1 in the DB

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes; // Maps the Inactivity Limit

    @Column(name = "human_body_enabled")
    private Boolean isHumanBodyEnabled; // Maps the human body visualization on the dashboard

}