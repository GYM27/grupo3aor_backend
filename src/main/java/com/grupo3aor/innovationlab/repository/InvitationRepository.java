package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    Optional<Invitation> findByEmail(String email);
    void deleteByEmail(String email);
    boolean existsByEmail(String email);
}
