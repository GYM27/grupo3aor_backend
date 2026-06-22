package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing simulations in the database.
 */
@Repository
public interface SimulationRepository extends JpaRepository<Simulation, UUID> {

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Simulation s LEFT JOIN FETCH s.scenario WHERE s.id = :id")
    java.util.Optional<Simulation> findById(@org.springframework.data.repository.query.Param("id") UUID id);

    /**
     * I created this custom query so we can easily fetch simulations based on their current status.
     * Perfect for building dashboards showing "Running Simulations"!
     * 
     * @param status The target simulation status
     * @return List of matching simulations
     */
    List<Simulation> findAllByStatus(SimulationStatus status);

    /**
     * Fetches simulations matching any of the provided statuses in a single query.
     * Used by the engine to retrieve both INICIADA and EM_CURSO in one DB round-trip.
     *
     * @param statuses Collection of statuses to match
     * @return List of matching simulations
     */
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Simulation s JOIN FETCH s.scenario WHERE s.status IN :statuses")
    List<Simulation> findAllByStatusIn(@org.springframework.data.repository.query.Param("statuses") Collection<SimulationStatus> statuses);

}
