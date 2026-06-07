package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * I created this JPA Entity to track the actual execution of a clinical scenario.
 */
@Entity
@Table(name = "simulations")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    // I need to know exactly when the simulation started and ended.
    // =========================================================
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // =========================================================
    // STATUS
    // I used an Enum to track if it's running, finished, or canceled.
    // Saved as a String to keep the DB readable!
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationStatus status;

    // =========================================================
    // IDENTITY (EQUALS & HASHCODE)
    // Same rule as always: I only compare by the primary key (id).
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
