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

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"system"})
    List<Rule> findByActiveTrue();
    
    List<Rule> findByActive(boolean active);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Rule r WHERE " +
            "(:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:systemId IS NULL OR r.system.id = :systemId) AND " +
            "(:active IS NULL OR r.active = :active) AND " +
            "r.deleted = :deleted")
    org.springframework.data.domain.Page<Rule> findFilteredRules(
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("systemId") Long systemId,
            @org.springframework.data.repository.query.Param("active") Boolean active,
            @org.springframework.data.repository.query.Param("deleted") boolean deleted,
            org.springframework.data.domain.Pageable pageable);

}
