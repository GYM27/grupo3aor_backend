package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Object-Relational Mapping (ORM) entity representing patient biometric readings.
 * Refactored to align with the core Auditable and Soft Delete architecture.
 * FK relation to Simulation enforces referential integrity at DB level.
 */
@Entity
@Table(name = "PHYSIOLOGICAL_READING")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PhysiologicalReading extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // I replaced the raw UUID field with a proper @ManyToOne relation to the Simulation entity.
    // This creates a real foreign key in the database, preventing orphan readings from being created
    // for simulations that do not exist.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    @ToString.Exclude
    private Simulation simulation;

    @Column(name = "handle", nullable = false)
    private String handle;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "\"value\"", nullable = false)
    private Double value;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhysiologicalReading)) return false;
        PhysiologicalReading other = (PhysiologicalReading) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}