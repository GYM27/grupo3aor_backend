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

import java.util.UUID;

/**
 * JPA entity enclosing textual and downloadable binary evaluations of executed simulations.
 * Refactored to align with the core Auditable and Soft Delete architecture.
 * FK relation to Simulation enforces referential integrity at DB level.
 */
@Entity
@Table(name = "EVALUATION_REPORT")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE evaluation_report SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
public class EvaluationReport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // I replaced the raw UUID field with a proper @ManyToOne relation to the Simulation entity.
    // This creates a real foreign key in the database, preventing orphan reports from being created
    // for simulations that do not exist.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    @ToString.Exclude
    private Simulation simulation;

    @Column(name = "intervalo_temporal", nullable = false)
    private String intervaloTemporal;

    @Lob
    @Column(name = "rationale_text", nullable = false, columnDefinition = "TEXT")
    private String rationaleText;

    @Lob
    @Column(name = "pdf_content", nullable = false)
    private byte[] pdfContent;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationReport)) return false;
        EvaluationReport other = (EvaluationReport) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}