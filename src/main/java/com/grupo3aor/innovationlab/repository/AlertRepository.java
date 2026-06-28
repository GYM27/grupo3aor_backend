package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Alert entities and performing database queries.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    /**
     * Retrieves all alerts associated with a specific simulation session.
     * Uses Spring Data's nested property traversal (simulation.id) through the @ManyToOne relation.
     *
     * @param simulationId the unique identifier of the simulation
     * @return a list of alerts triggered during the simulation
     */
    @Query("SELECT DISTINCT a FROM Alert a JOIN FETCH a.simulation JOIN FETCH a.rule r JOIN FETCH r.system WHERE a.simulation.id = :simulationId AND a.active = true")
    List<Alert> findBySimulation_Id(@Param("simulationId") UUID simulationId);

    /**
     * Fetches alerts for a simulation up to a specific cutoff timestamp.
     * Uses the existing idx_alert_sim index on (simulation_id, timestamp).
     * This allows PDF generation to filter by time WITHOUT waiting for bulkDelete to complete.
     */
    @Query("SELECT DISTINCT a FROM Alert a JOIN FETCH a.simulation JOIN FETCH a.rule r JOIN FETCH r.system WHERE a.simulation.id = :simulationId AND a.active = true AND a.timestamp <= :cutOff")
    List<Alert> findBySimulationIdUpToCutoff(@Param("simulationId") UUID simulationId, @Param("cutOff") java.time.LocalDateTime cutOff);

    // I added this query to prevent alert spam. It checks if there's already an active alert for the same simulation and rule.
    boolean existsBySimulationAndRuleAndStatus(Simulation simulation, Rule rule, AlertStatus status);
    
    // I added this query to fetch the actual active alert so we can update its warningAt and resolvedAt timestamps.
    Alert findFirstBySimulationAndRuleAndStatusOrderByTimestampDesc(Simulation simulation, Rule rule, AlertStatus status);
    
    /**
     * Bulk deletes alerts that occur after a specific timestamp.
     * Uses @Modifying and @Query to prevent N+1 select/delete problems.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Alert a SET a.active = false, a.updatedAt = CURRENT_TIMESTAMP WHERE a.simulation.id = :simId AND a.timestamp > :timestamp")
    void bulkDeleteFutureAlerts(
            @org.springframework.data.repository.query.Param("simId") UUID simId, 
            @org.springframework.data.repository.query.Param("timestamp") java.time.LocalDateTime timestamp
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Alert a SET a.warningAt = null WHERE a.simulation.id = :simId AND a.warningAt > :timestamp")
    void clearFutureWarnings(
            @org.springframework.data.repository.query.Param("simId") UUID simId, 
            @org.springframework.data.repository.query.Param("timestamp") java.time.LocalDateTime timestamp
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Alert a SET a.resolvedAt = null WHERE a.simulation.id = :simId AND a.resolvedAt > :timestamp")
    void clearFutureResolutions(
            @org.springframework.data.repository.query.Param("simId") UUID simId, 
            @org.springframework.data.repository.query.Param("timestamp") java.time.LocalDateTime timestamp
    );
}
