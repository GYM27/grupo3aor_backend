package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access operations interface managing persistent routines for {@link PhysiologicalSystem} entries.
 * <p>
 * I engineered this abstraction layer to manage physiological system boundaries,
 * keeping separate tracking methods for active operational workflows and logical recycling dashboards.
 * </p>
 */
@Repository
public interface PhysiologicalSystemRepository extends JpaRepository<PhysiologicalSystem, Long> {

    /**
     * Resolves an operational system matching a distinct unique name representation.
     * @param systemName Structural system text key identifier.
     * @return An Optional wrapper enclosing the matching record.
     */
    Optional<PhysiologicalSystem> findBySystemName(String systemName);

    /**
     * Gathers all operational physiological systems, omitting soft-deleted rows.
     * @return Collection of active setups ready for simulation workflows.
     */
    // I introduced this distinct query method to bypass soft-deleted rows during 
    // everyday simulation setup lists displayed to operational users.
    List<PhysiologicalSystem> findAllByActiveTrue();

    /**
     * Gathers exclusively soft-deleted or archived physiological systems.
     * @return Collection of deactivated rows reserved for administrative lookup.
     */
    // I built this query specifically for administrative dashboards, allowing operators 
    // to inspect archived setups or execute rollback restoration operations.
    List<PhysiologicalSystem> findAllByActiveFalse();
}
