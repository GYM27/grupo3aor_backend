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
import org.springframework.data.domain.Persistable;

/**
 * Object-Relational Mapping (ORM) entity representing patient biometric readings.
 * Refactored to align with the core Auditable and Soft Delete architecture.
 * FK relation to Simulation enforces referential integrity at DB level.
 */
@Entity
@Table(name = "PHYSIOLOGICAL_READING", indexes = {
    @Index(name = "idx_reading_sim_time", columnList = "simulation_id, timestamp")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PhysiologicalReading extends Auditable implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @Builder.Default // O Lombok vai usar este valor por defeito se ninguém passar um ID
    private UUID id = UUID.randomUUID(); // Gera o ID automaticamente na RAM mal o objeto é instanciado!

    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

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

    // --- MÉTODOS DO PERSISTABLE PARA OTIMIZAR O SPRING DATA JPA ---

    @Override
    public boolean isNew() {
        return this.isNewRecord; // Diz ao Spring para fazer sempre INSERT e nunca SELECT
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNewRecord = false; // Após salvar ou carregar da BD, deixa de ser novo
    }

    // --------------------------------------------------------------

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