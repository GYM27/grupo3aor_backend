package com.grupo3aor.innovationlab.bootstrap;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.repository.PhysiologicalSystemRepository;
import com.grupo3aor.innovationlab.repository.GlobalSettingsRepository;
import com.grupo3aor.innovationlab.domain.entity.GlobalSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

/**
 * Automatically seeds the database with essential physiological systems if they don't exist yet.
 */
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final PhysiologicalSystemRepository physiologicalSystemRepository;
    private final GlobalSettingsRepository globalSettingsRepository;
    private final RuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedPhysiologicalSystems();
        seedGlobalSettings();
        seedRules();
        seedUsers();
    }

    private void seedPhysiologicalSystems() {
        if (physiologicalSystemRepository.count() == 0) {
            System.out.println("[SEEDER] Inserindo Sistemas Fisiológicos iniciais...");
            
            List<PhysiologicalSystem> systems = List.of(
                PhysiologicalSystem.builder().systemName("Sistema Cardiovascular").active(true).createdBy("system_seeder").updatedBy("system_seeder").build(),
                PhysiologicalSystem.builder().systemName("Sistema Respiratório").active(true).createdBy("system_seeder").updatedBy("system_seeder").build(),
                PhysiologicalSystem.builder().systemName("Sistema Neurológico").active(true).createdBy("system_seeder").updatedBy("system_seeder").build()
            );
            
            physiologicalSystemRepository.saveAll(systems);
            System.out.println("[SEEDER] Sistemas Fisiológicos criados com sucesso!");
        }
    }

    private void seedGlobalSettings() {
        if (globalSettingsRepository.count() == 0) {
            System.out.println("[SEEDER] Inserindo Configurações Globais iniciais...");
            
            GlobalSettings defaultSettings = GlobalSettings.builder()
            .id(1L)
            .sessionTimeoutMinutes(30)
            .isHumanBodyEnabled(true)
            .isDbFailed(false)
            .createdBy("system_seeder")
            .updatedBy("system_seeder")
            .build();

            globalSettingsRepository.save(defaultSettings);
            System.out.println("[SEEDER] Configurações Globais (ID=1) criadas com sucesso!");
        }
    }

    private void seedRules() {
        if (ruleRepository.count() == 0) {
            System.out.println("[SEEDER] Inserindo Regras Clínicas iniciais para Asma...");
            
            PhysiologicalSystem cardio = physiologicalSystemRepository.findAll().stream()
                .filter(s -> s.getSystemName().contains("Cardiovascular"))
                .findFirst()
                .orElse(null);

            PhysiologicalSystem resp = physiologicalSystemRepository.findAll().stream()
                .filter(s -> s.getSystemName().contains("Respiratório"))
                .findFirst()
                .orElse(null);

            if (cardio != null && resp != null) {
                // 1. Taquicardia
                com.grupo3aor.innovationlab.domain.entity.Rule hrRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Taquicardia (HR > 95)")
                    .expressionDsl("{\"metric\": \"HEART_RATE\", \"operator\": \">\", \"threshold\": 95, \"persistence\": 30}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.ALERTA)
                    .system(cardio).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();
                
                // 2. Taquipneia
                com.grupo3aor.innovationlab.domain.entity.Rule rrRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Taquipneia Severa (RR > 30)")
                    .expressionDsl("{\"metric\": \"RR\", \"operator\": \">\", \"threshold\": 30, \"persistence\": 20}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO)
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                // 3. Hipoxia
                com.grupo3aor.innovationlab.domain.entity.Rule spo2Rule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Hipoxia (SpO2 < 97%)")
                    .expressionDsl("{\"metric\": \"SPO2\", \"operator\": \"<\", \"threshold\": 97, \"persistence\": 40}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.ALERTA)
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                // 4. Volume Corrente Baixo (Broncoconstrição)
                com.grupo3aor.innovationlab.domain.entity.Rule tidalRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Volume Corrente Baixo (< 200mL)")
                    .expressionDsl("{\"metric\": \"TidalVolume\", \"operator\": \"<\", \"threshold\": 200, \"persistence\": 15}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.ALERTA)
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                // 5. Acidose Respiratória
                com.grupo3aor.innovationlab.domain.entity.Rule phRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Acidose Respiratória (pH < 7.35)")
                    .expressionDsl("{\"metric\": \"ArterialBloodPH\", \"operator\": \"<\", \"threshold\": 7.35, \"persistence\": 60}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO)
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                ruleRepository.saveAll(java.util.List.of(hrRule, rrRule, spo2Rule, tidalRule, phRule));
                System.out.println("[SEEDER] 5 Regras Clínicas para o ficheiro AsthmaAttack criadas com sucesso!");
            }
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            System.out.println("[SEEDER] Inserindo Utilizadores iniciais...");

            com.grupo3aor.innovationlab.domain.entity.User luis = com.grupo3aor.innovationlab.domain.entity.User.builder()
                .firstName("Luís")
                .lastName("Admin")
                .email("luis@gmail.com")
                .passwordHash(passwordEncoder.encode("Pass1234#"))
                .perfil(com.grupo3aor.innovationlab.domain.enums.PerfilEnum.ADMIN)
                .accountActivated(true)
                .active(true)
                .createdBy("system_seeder")
                .updatedBy("system_seeder")
                .build();

            userRepository.save(luis);
            System.out.println("[SEEDER] Utilizador luis@gmail.com criado com sucesso!");
        }
    }
}
