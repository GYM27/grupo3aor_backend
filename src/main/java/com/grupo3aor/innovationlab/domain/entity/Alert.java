package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Database entity mapped to capture runtime threshold violation alerts.
 * Refactored to align with the core Auditable and Soft Delete architecture.
 * FK relations to Simulation and Rule enforce referential integrity at DB level.
 */
@Entity
@Table(name = "ALERT", indexes = {
    @Index(name = "idx_alert_sim", columnList = "simulation_id, timestamp")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Alert extends Auditable implements Persistable<UUID> {

    @Id
    // 2. Remover @GeneratedValue e usar @Builder.Default para gerar o ID atempadamente
    @Column(name = "id", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    // 3. Flag invisível para o Hibernate saber que é um INSERT direto
    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

    // I replaced the raw UUID field with a proper @ManyToOne relation to the Simulation entity.
    // This creates a real foreign key in the database, preventing orphan alerts from being created
    // for simulations that do not exist — which was the main gap identified in the integrity review.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    @ToString.Exclude
    private Simulation simulation;

    // I replaced the raw UUID field with a proper @ManyToOne relation to the Rule entity.
    // This ensures alerts can only reference rules that actually exist in the system.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @ToString.Exclude
    private Rule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Column(name = "value_at_trigger", nullable = false)
    private Double valueAtTrigger;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // --- MÉTODOS DO PERSISTABLE PARA OTIMIZAR O SPRING DATA JPA ---
    
    @Override
    public boolean isNew() {
        return this.isNewRecord;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNewRecord = false;
    }

    // --------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alert)) return false;
        Alert other = (Alert) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}