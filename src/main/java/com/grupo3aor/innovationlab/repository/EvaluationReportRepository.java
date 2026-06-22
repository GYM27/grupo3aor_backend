package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for handling database operations concerning Evaluation Reports.
 */
@Repository
public interface EvaluationReportRepository extends JpaRepository<EvaluationReport, UUID> {

    /**
     * Finds the evaluation report associated with a specific simulation.
     * Uses Spring Data's nested property traversal (simulation.id) through the @ManyToOne relation.
     *
     * @param simulationId the unique identifier of the simulation
     * @return an Optional containing the evaluation report if found
     */
    Optional<EvaluationReport> findFirstBySimulation_IdOrderByCreatedAtDesc(UUID simulationId);
}