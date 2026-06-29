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
     * Finds all evaluation reports associated with a specific simulation, ordered by creation date descending.
     * Uses Spring Data's nested property traversal (simulation.id) through the @ManyToOne relation.
     *
     * @param simulationId the unique identifier of the simulation
     * @return a List containing the evaluation reports
     */
    java.util.List<EvaluationReport> findBySimulation_IdOrderByCreatedAtDesc(UUID simulationId);
}