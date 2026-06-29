package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tarefa agendada para manter a base de dados limpa, removendo convites que já expiraram.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationCleanupTask {

    private final InvitationRepository invitationRepository;

    /**
     * Executa todos os dias à meia-noite (00:00:00).
     * O formato Cron é: Segundos Minutos Horas Dia-do-Mês Mês Dia-da-Semana
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("[MAINTENANCE] Iniciando limpeza de convites expirados...");
        
        int deletedCount = invitationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        
        if (deletedCount > 0) {
            log.info("[MAINTENANCE] Limpeza concluída: {} convite(s) expirado(s) removido(s) da base de dados.", deletedCount);
        } else {
            log.info("[MAINTENANCE] Limpeza concluída: Nenhum convite expirado encontrado.");
        }
    }
}
