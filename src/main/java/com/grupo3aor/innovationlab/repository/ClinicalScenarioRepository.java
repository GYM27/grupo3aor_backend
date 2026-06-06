package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access operations interface managing persistent routines for {@link ClinicalScenario} entries.
 * <p>
 * I engineered this abstraction layer to manage clinical configuration access boundaries,
 * keeping separate tracking methods for active operational workflows and logical recycling dashboards.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Repository
public interface ClinicalScenarioRepository extends JpaRepository<ClinicalScenario, Long> {

    /**
     * Resolves an operational scenario matching a distinct unique name representation.
     * * @param name Structural scenario text key identifier.
     * @return An Optional wrapper enclosing the matching record.
     */
    Optional<ClinicalScenario> findByName(String name);

    /**
     * Gathers all operational clinical configurations, omitting soft-deleted rows.
     * * @return Collection of active setups ready for simulation workflows.
     */
    // I introduced this distinct query method to bypass soft-deleted rows during 
    // everyday simulation setup lists displayed to operational users.
    List<ClinicalScenario> findAllByActiveTrue();

    /**
     * Gathers exclusively soft-deleted or archived clinical configurations.
     * * @return Collection of deactivated rows reserved for administrative lookup.
     */
    // I built this query specifically for administrative dashboards, allowing operators 
    // to inspect archived setups or execute rollback restoration operations.
    List<ClinicalScenario> findAllByActiveFalse();
}
