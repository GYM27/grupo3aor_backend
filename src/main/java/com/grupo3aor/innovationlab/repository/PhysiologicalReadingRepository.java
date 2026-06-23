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

    /**
     * Finds all readings for a simulation ordered chronologically.
     */
    List<PhysiologicalReading> findBySimulation_IdOrderByTimestampAsc(UUID simulationId);

    /**
     * Finds the first reading of a simulation to determine the exact base timestamp.
     */
    PhysiologicalReading findFirstBySimulation_IdOrderByTimestampAsc(UUID simulationId);

    /**
     * Finds the last reading of a simulation to determine the exact end timestamp.
     */
    PhysiologicalReading findFirstBySimulation_IdOrderByTimestampDesc(UUID simulationId);

    /**
     * Finds the top 20 latest readings of a simulation.
     */
    List<PhysiologicalReading> findTop20BySimulation_IdOrderByTimestampDesc(UUID simulationId);

    /**
     * Bulk deletes readings that occur after a specific timestamp.
     * Uses @Modifying and @Query to prevent N+1 select/delete problems.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE PhysiologicalReading p SET p.active = false, p.updatedAt = CURRENT_TIMESTAMP WHERE p.simulation.id = :simId AND p.timestamp > :timestamp")
    void bulkDeleteFutureReadings(
            @org.springframework.data.repository.query.Param("simId") UUID simId, 
            @org.springframework.data.repository.query.Param("timestamp") java.time.LocalDateTime timestamp
    );
}