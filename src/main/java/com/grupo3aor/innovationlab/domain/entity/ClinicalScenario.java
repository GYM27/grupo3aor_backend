package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;

/**
 * Persistence model mapping our clinical scenarios registry.
 * <p>
 * I engineered this entity to track medical simulation setups, ensuring every scenario 
 * has proper descriptive boundaries and immutable auditing metadata attached to it.
 * I refactored it to extend {@link Auditable}, eliminating duplicated audit columns
 * and guaranteeing structural consistency across the entire domain model.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Entity
@Table(name = "clinical_scenarios")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// I enforced soft deletion here as well to guarantee that no clinical setups 
// are ever permanently destroyed, keeping them available for historical reporting.
@SQLDelete(sql = "UPDATE clinical_scenarios SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
public class ClinicalScenario extends Auditable {

    /**
     * Internal autoincrement primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The title of the clinical scenario.
     */
    @NotBlank(message = "Scenario name cannot be empty")
    @Size(max = 150, message = "Scenario name cannot exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Detailed medical description of the scenario.
     */
    // I mapped this description to a TEXT column using Lob to allow 
    // extensive medical documentation without breaking the string length boundaries.
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Soft delete operational indicator.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClinicalScenario)) return false;
        ClinicalScenario other = (ClinicalScenario) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
