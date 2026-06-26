package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleCacheService {

    private final RuleRepository ruleRepository;
    private List<Rule> cachedActiveRules = new ArrayList<>();

    // Pre-loading the cache as soon as the application spins up
    @PostConstruct
    public void init() {
        refreshCache();
    }

    // Refreshing the rules cache every minute in the background
    @Scheduled(fixedRate = 60000)
    public void refreshCache() {
        try {
            cachedActiveRules = ruleRepository.findByActiveTrue();
            log.debug("Cache de regras atualizada: {} regras ativas.", cachedActiveRules.size());
        } catch (Exception e) {
            log.warn("[CACHE] Não foi possível atualizar regras. A usar a última versão conhecida.");
        }
    }

    // The Rules Engine relies on this in-memory list instead of hitting the database on every tick!
    public List<Rule> getActiveRules() {
        return cachedActiveRules;
    }
}