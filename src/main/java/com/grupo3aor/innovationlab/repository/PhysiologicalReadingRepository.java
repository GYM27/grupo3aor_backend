package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing PhysiologicalReading entities in the database.
 */
@Repository
public interface PhysiologicalReadingRepository extends JpaRepository<PhysiologicalReading, UUID> {

    /**
     * Finds all physiological readings linked to a specific active simulation.
     * Uses Spring Data's nested property traversal (simulation.id) through the @ManyToOne relation.
     *
     * @param simulationId the unique identification of the simulation
     * @return a list of physiological readings
     */
    List<PhysiologicalReading> findBySimulation_Id(UUID simulationId);
}