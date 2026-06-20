package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.dto.RuleCondition;

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
@SuperBuilder
// I updated this interceptor to use 'active = false' for soft delete,
// standardizing the naming across all entities (User, PhysiologicalSystem, ClinicalScenario all use 'active').
@SQLDelete(sql = "UPDATE rules SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
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
    // I added a name field to identify the rule in the frontend easily.
    // I used columnDefinition = "TEXT" because the DSL expression might be quite long.
    // =========================================================
    @Column(name = "name")
    private String name;

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
    // Instead of completely wiping rules from the DB, I just flag them as inactive.
    // Renamed from 'deleted' to 'active' to be consistent with User, PhysiologicalSystem
    // and ClinicalScenario — all of which use active=true/false for soft delete.
    // =========================================================
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // =========================================================
    // DOMAIN LOGIC (DDD)
    // I moved the math logic here so the entity knows its own limits!
    // =========================================================
    @Transient
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public boolean isTriggeredBy(String handle, Double value) {
        if (this.expressionDsl == null || this.expressionDsl.isEmpty()) return false;
        try {
            RuleCondition condition = MAPPER.readValue(this.expressionDsl, RuleCondition.class);
            if (!condition.getMetric().equals(handle)) return false;

            switch (condition.getOperator()) {
                case ">": return value.compareTo(condition.getThreshold()) > 0;
                case "<": return value.compareTo(condition.getThreshold()) < 0;
                case "==": return value.compareTo(condition.getThreshold()) == 0;
                default: return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

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
