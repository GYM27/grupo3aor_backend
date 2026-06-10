package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<Alert> findBySimulation_Id(UUID simulationId);
}