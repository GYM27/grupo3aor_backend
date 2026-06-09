package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing rules in the database.
 * <p>
 * I added a custom query to exclusively find rules that haven't been soft-deleted.
 * </p>
 */
@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {

    /**
     * Fetches all active rules, ignoring soft-deleted ones.
     * Renamed from findAllByDeletedFalse to findAllByActiveTrue to be consistent
     * with UserRepository, PhysiologicalSystemRepository and ClinicalScenarioRepository.
     * @return List of active (non-deleted) rules
     */
    List<Rule> findAllByActiveTrue();

}
