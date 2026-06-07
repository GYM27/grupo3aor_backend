package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.util.UUID;

/**
 * I created this JPA Entity to represent the "rules" table.
 * This is where we store the logic for triggering alerts during a simulation.
 */
@Entity
@Table(name = "rules")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
// I added the SQLDelete interceptor to ensure Soft Delete works perfectly at the database level!
@SQLDelete(sql = "UPDATE rules SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Rule extends Auditable {

    // =========================================================
    // MY PRIMARY KEY
    // I decided to use UUID here just like the UML suggested, 
    // it's safer and avoids conflicts if our system ever goes offline!
    // =========================================================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =========================================================
    // THE RULE LOGIC
    // I used columnDefinition = "TEXT" because the DSL expression might be quite long.
    // =========================================================
    @Column(name = "expression_dsl", columnDefinition = "TEXT")
    private String expressionDsl;

    // =========================================================
    // THE SEVERITY
    // I chose EnumType.STRING to save "ALERTA" or "CRITICO" in the DB, 
    // making it much easier to read than just numbers.
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    // =========================================================
    // THE PHYSIOLOGICAL SYSTEM
    // I mapped this to the actual PhysiologicalSystem entity to guarantee
    // database referential integrity!
    // =========================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    @ToString.Exclude
    private PhysiologicalSystem system;

    // =========================================================
    // WHO CREATED THIS RULE
    // I mapped this to the existing User entity. Notice the @ManyToOne, 
    // because many rules can be created by a single user!
    // =========================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @ToString.Exclude // I excluded this from ToString to prevent infinite loops when printing objects
    private User createdByUser;

    // =========================================================
    // MY SOFT DELETE
    // Instead of completely wiping rules from the DB, I just flag them as deleted.
    // This way we never break historical simulations that used this rule!
    // =========================================================
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // =========================================================
    // IDENTITY (EQUALS & HASHCODE)
    // I focused strictly on the 'id' to keep collections stable.
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rule)) return false;
        Rule other = (Rule) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
