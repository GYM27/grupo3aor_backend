package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;

/**
 * Persistence model mapping our physiological systems registry.
 * <p>
 * I designed this structure to store the different physiological systems
 * managed by the Innovation Lab, maintaining strict auditing metadata 
 * and structural validation. I refactored it to extend {@link Auditable},
 * centralizing all audit columns in a single inheritance chain.
 * </p>
 */
@Entity
@Table(name = "physiological_systems")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// I configured this interceptor to rewrite the default physical deletion behavior.
// This ensures that whenever a delete instruction is issued, the platform alters
// the active flag instead of wiping the record, thus preserving structural history.
@SQLDelete(sql = "UPDATE physiological_systems SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
public class PhysiologicalSystem extends Auditable {

    /**
     * Internal autoincrement primary key.
     */
    // I opted to maintain Long as our primary key format to keep absolute consistency 
    // across all database schemas within the project.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the physiological system.
     */
    // I applied a 100-character boundary restriction here to shield the database 
    // against unexpected payload sizes or field inflation vulnerabilities.
    @NotBlank(message = "System name cannot be empty")
    @Size(max = 100, message = "System name cannot exceed 100 characters")
    @Column(name = "system_name", nullable = false, length = 100, unique = true)
    private String systemName;

    /**
     * Soft delete operational indicator.
     */
    // I chose a primitive boolean flag to handle logical deletion safely, preventing 
    // accidental losses while keeping historical records accessible for internal tools.
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhysiologicalSystem)) return false;
        PhysiologicalSystem other = (PhysiologicalSystem) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
