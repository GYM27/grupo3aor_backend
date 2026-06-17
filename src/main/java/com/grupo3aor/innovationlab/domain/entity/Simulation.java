package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity tracking the actual execution of a clinical scenario.
 */
@Entity
@Table(name = "simulations")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Simulation extends Auditable {

    // =========================================================
    // MY PRIMARY KEY
    // Using UUID here to ensure uniqueness across distributed systems,
    // exactly like we planned for the Degraded Mode bonus feature!
    // =========================================================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =========================================================
    // THE SCENARIO REFERENCE
    // Mapped to the real ClinicalScenario entity to ensure we only
    // run simulations for scenarios that actually exist in the DB!
    // =========================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    @ToString.Exclude
    private ClinicalScenario scenario;

    // =========================================================
    // THE USER EXECUTING IT
    // This links to the User entity. It tells us who actually pressed the "start" button.
    // =========================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    // =========================================================
    // TIMESTAMPS
    // We need to know exactly when the simulation started and ended.
    // =========================================================
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // =========================================================
    // STATUS
    // Using an Enum to track if it's running, finished, or canceled.
    // Saved as a String to keep the DB readable!
    // We changed the column name to sim_status to bypass H2 ENUM strictness on the old table.
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "sim_status", nullable = false, columnDefinition = "varchar(50) default 'FINALIZADA'")
    @Builder.Default
    private SimulationStatus status = SimulationStatus.FINALIZADA;

    // =========================================================
    // PROGRESS TRACKING
    // Tracking where we are in the JSON metrics array during playback.
    // =========================================================
    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int nextMetricIndex = 0;

    // =========================================================
    // IDENTITY (EQUALS & HASHCODE)
    // Same rule as always: we only compare by the primary key (id).
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Simulation)) return false;
        Simulation other = (Simulation) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
