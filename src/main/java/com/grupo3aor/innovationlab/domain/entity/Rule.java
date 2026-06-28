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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
// I updated this interceptor to use 'deleted = true' for soft delete,
// so 'active' can be toggled without deleting the rule.
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
    // I added a name field to identify the rule in the frontend easily.
    // I used columnDefinition = "TEXT" because the DSL expression might be quite long.
    // =========================================================
    @Column(name = "name")
    private String name;

    @Column(name = "expression_dsl", columnDefinition = "TEXT")
    private String expressionDsl;

    public void setExpressionDsl(String expressionDsl) {
        this.expressionDsl = expressionDsl;
        this.cachedCondition = null;
    }

    // =========================================================
    // THE SEVERITY
    // I chose EnumType.STRING to save "ALERTA" or "CRITICO" in the DB, 
    // making it much easier to read than just numbers.
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;
    
    @Column(name = "analytical_justification", columnDefinition = "TEXT")
    private String analyticalJustification;

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
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    // True soft-delete flag, introduced so we can separate 'inactive' from 'deleted'
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean deleted = false;

    // =========================================================
    // DOMAIN LOGIC (DDD)
    // I moved the math logic here so the entity knows its own limits!
    // =========================================================
    @Transient
    private static final ObjectMapper MAPPER = new YAMLMapper();

    @Transient
    private RuleCondition cachedCondition;

    public boolean isApplicableTo(String handle) {
        if (this.expressionDsl == null || this.expressionDsl.isEmpty()) return false;
        try {
            if (this.cachedCondition == null) {
                this.cachedCondition = MAPPER.readValue(this.expressionDsl, RuleCondition.class);
            }
            String metric = this.cachedCondition.getMetric();
            if (metric == null || handle == null) return false;
            
            if ("HEART_RATE".equalsIgnoreCase(metric)) {
                return "HeartRate".equalsIgnoreCase(handle) || "Cardiovascular".equalsIgnoreCase(handle) || "HR".equalsIgnoreCase(handle);
            } else if ("SPO2".equalsIgnoreCase(metric)) {
                return "OxygenSaturation".equalsIgnoreCase(handle) || "SpO2".equalsIgnoreCase(handle) || "Respiratory".equalsIgnoreCase(handle);
            } else if ("BP".equalsIgnoreCase(metric)) {
                return "ArterialPressure_Systolic".equalsIgnoreCase(handle) || "SystolicArterialPressure".equalsIgnoreCase(handle) || "SBP".equalsIgnoreCase(handle);
            } else if ("RR".equalsIgnoreCase(metric)) {
                return "RespirationRate".equalsIgnoreCase(handle);
            } else if ("TEMP".equalsIgnoreCase(metric) || "TEMPERATURE".equalsIgnoreCase(metric)) {
                return "CoreTemperature".equalsIgnoreCase(handle) || "TEMP".equalsIgnoreCase(handle);
            } else {
                return metric.equalsIgnoreCase(handle);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTriggeredBy(String handle, Double value) {
        if (this.expressionDsl == null || this.expressionDsl.isEmpty()) return false;
        try {
            if (this.cachedCondition == null) {
                this.cachedCondition = MAPPER.readValue(this.expressionDsl, RuleCondition.class);
            }
            
            String metric = this.cachedCondition.getMetric();
            boolean matches = false;
            
            if ("HEART_RATE".equalsIgnoreCase(metric)) {
                matches = "HeartRate".equalsIgnoreCase(handle) || "Cardiovascular".equalsIgnoreCase(handle) || "HR".equalsIgnoreCase(handle);
            } else if ("SPO2".equalsIgnoreCase(metric)) {
                matches = "OxygenSaturation".equalsIgnoreCase(handle) || "SpO2".equalsIgnoreCase(handle) || "Respiratory".equalsIgnoreCase(handle);
            } else if ("BP".equalsIgnoreCase(metric)) {
                matches = "ArterialPressure_Systolic".equalsIgnoreCase(handle) || "SystolicArterialPressure".equalsIgnoreCase(handle) || "SBP".equalsIgnoreCase(handle);
            } else if ("RR".equalsIgnoreCase(metric)) {
                matches = "RespirationRate".equalsIgnoreCase(handle);
            } else if ("TEMP".equalsIgnoreCase(metric) || "TEMPERATURE".equalsIgnoreCase(metric)) {
                matches = "CoreTemperature".equalsIgnoreCase(handle) || "TEMP".equalsIgnoreCase(handle);
            } else {
                matches = metric != null && metric.equalsIgnoreCase(handle);
            }

            if (!matches) return false;

            if (this.cachedCondition.getActivationThreshold() == null) return false;

            switch (this.cachedCondition.getOperator()) {
                case ">": return value.compareTo(this.cachedCondition.getActivationThreshold()) > 0;
                case "<": return value.compareTo(this.cachedCondition.getActivationThreshold()) < 0;
                case "==": return value.compareTo(this.cachedCondition.getActivationThreshold()) == 0;
                default: return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isResolvedBy(String handle, Double value) {
        if (this.expressionDsl == null || this.expressionDsl.isEmpty()) return false;
        try {
            if (this.cachedCondition == null) {
                this.cachedCondition = MAPPER.readValue(this.expressionDsl, RuleCondition.class);
            }
            
            String metric = this.cachedCondition.getMetric();
            boolean matches = false;
            
            if ("HEART_RATE".equalsIgnoreCase(metric)) {
                matches = "HeartRate".equalsIgnoreCase(handle) || "Cardiovascular".equalsIgnoreCase(handle) || "HR".equalsIgnoreCase(handle);
            } else if ("SPO2".equalsIgnoreCase(metric)) {
                matches = "OxygenSaturation".equalsIgnoreCase(handle) || "SpO2".equalsIgnoreCase(handle) || "Respiratory".equalsIgnoreCase(handle);
            } else if ("BP".equalsIgnoreCase(metric)) {
                matches = "ArterialPressure_Systolic".equalsIgnoreCase(handle) || "SystolicArterialPressure".equalsIgnoreCase(handle) || "SBP".equalsIgnoreCase(handle);
            } else if ("RR".equalsIgnoreCase(metric)) {
                matches = "RespirationRate".equalsIgnoreCase(handle);
            } else if ("TEMP".equalsIgnoreCase(metric) || "TEMPERATURE".equalsIgnoreCase(metric)) {
                matches = "CoreTemperature".equalsIgnoreCase(handle) || "TEMP".equalsIgnoreCase(handle);
            } else {
                matches = metric != null && metric.equalsIgnoreCase(handle);
            }

            if (!matches) return false;

            if (this.cachedCondition.getResolutionThreshold() == null) return true; // If no resolution defined, act as if resolved.

            // The operator for resolution is implicitly the opposite for > and <, but we can also just check explicitly:
            // If the activation was ">", resolution is when it drops below the resolutionThreshold.
            switch (this.cachedCondition.getOperator()) {
                case ">": return value.compareTo(this.cachedCondition.getResolutionThreshold()) < 0;
                case "<": return value.compareTo(this.cachedCondition.getResolutionThreshold()) > 0;
                // For == or outside bounds, if resolution is not clear, we rely on the threshold or assume false.
                case "==": return value.compareTo(this.cachedCondition.getResolutionThreshold()) != 0;
                default: return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getActivationPersistence() {
        if (this.cachedCondition == null) {
            if (this.expressionDsl != null && !this.expressionDsl.isEmpty()) {
                try {
                    this.cachedCondition = MAPPER.readValue(this.expressionDsl, RuleCondition.class);
                } catch (Exception e) {
                    return 0;
                }
            } else {
                return 0;
            }
        }
        return this.cachedCondition.getActivationPersistence() != null ? this.cachedCondition.getActivationPersistence() : 0;
    }

    public Integer getResolutionPersistence() {
        if (this.cachedCondition == null) {
            if (this.expressionDsl != null && !this.expressionDsl.isEmpty()) {
                try {
                    this.cachedCondition = MAPPER.readValue(this.expressionDsl, RuleCondition.class);
                } catch (Exception e) {
                    return 0;
                }
            } else {
                return 0;
            }
        }
        return this.cachedCondition.getResolutionPersistence() != null ? this.cachedCondition.getResolutionPersistence() : 0;
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
