package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

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
    @Query("SELECT r FROM PhysiologicalReading r JOIN FETCH r.simulation WHERE r.simulation.id = :simulationId ORDER BY r.timestamp ASC")
    List<PhysiologicalReading> findBySimulation_Id(@Param("simulationId") UUID simulationId);

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
     * Finds readings for a simulation within a specific time window.
     * Essential for high-performance snapshot extraction during PDF generation.
     */
    @Query(
        "SELECT r FROM PhysiologicalReading r " +
        "WHERE r.simulation.id = :simId " +
        "AND r.timestamp >= :startTime " +
        "AND r.timestamp <= :endTime " +
        "ORDER BY r.timestamp DESC"
    )
    List<PhysiologicalReading> findSnapshotWindow(
            @Param("simId") UUID simId, 
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Bulk deletes readings that occur after a specific timestamp.
     * Uses @Modifying and @Query to prevent N+1 select/delete problems.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PhysiologicalReading p SET p.active = false, p.updatedAt = CURRENT_TIMESTAMP WHERE p.simulation.id = :simId AND p.timestamp > :timestamp")
    void bulkDeleteFutureReadings(
            @Param("simId") UUID simId, 
            @Param("timestamp") LocalDateTime timestamp
    );
}