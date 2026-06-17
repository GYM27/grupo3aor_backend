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
    @Query("SELECT a FROM Alert a JOIN FETCH a.simulation JOIN FETCH a.rule WHERE a.simulation.id = :simulationId")
    List<Alert> findBySimulation_Id(@Param("simulationId") UUID simulationId);

    // I added this query to prevent alert spam. It checks if there's already an active alert for the same simulation and rule.
    boolean existsBySimulationAndRuleAndStatus(Simulation simulation, Rule rule, AlertStatus status);
    
}
