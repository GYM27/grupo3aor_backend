package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task to keep the database clean by removing expired invitations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationCleanupTask {

    private final InvitationRepository invitationRepository;

    /**
     * Executes every day at midnight (00:00:00).
     * Cron format is: Seconds Minutes Hours Day-of-Month Month Day-of-Week
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("[MAINTENANCE] Starting cleanup of expired invitations...");
        
        int deletedCount = invitationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        
        if (deletedCount > 0) {
            log.info("[MAINTENANCE] Cleanup completed: {} expired invitation(s) removed from the database.", deletedCount);
        } else {
            log.info("[MAINTENANCE] Cleanup completed: No expired invitations found.");
        }
    }
}
