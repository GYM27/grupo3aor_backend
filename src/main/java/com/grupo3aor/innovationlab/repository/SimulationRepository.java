package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing simulations in the database.
 */
@Repository
public interface SimulationRepository extends JpaRepository<Simulation, UUID> {

    /**
     * I created this custom query so we can easily fetch simulations based on their current status.
     * Perfect for building dashboards showing "Running Simulations"!
     * 
     * @param status The target simulation status
     * @return List of matching simulations
     */
    List<Simulation> findAllByStatus(SimulationStatus status);

}
