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
            .createdBy("system_seeder")
            .updatedBy("system_seeder")
            .build();

            globalSettingsRepository.save(defaultSettings);
            System.out.println("[SEEDER] Configurações Globais (ID=1) criadas com sucesso!");
        }
    }

    private void seedRules() {
        // OVERRIDE: Update existing rules in the DB that don't have hysteresis yet (from older runs)
        List<com.grupo3aor.innovationlab.domain.entity.Rule> allRules = ruleRepository.findAll();
        boolean updatedAny = false;
        for (com.grupo3aor.innovationlab.domain.entity.Rule r : allRules) {
            String dsl = r.getExpressionDsl();
            
            // Força a atualização da Taquicardia para testar o Alerta Crítico que resolve
            if (r.getName() != null && r.getName().contains("Taquicardia") && (!dsl.contains("110") || r.getSeverity() != com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO)) {
                r.setName("Taquicardia (HR > 105)");
                r.setExpressionDsl("{\"metric\": \"HEART_RATE\", \"operator\": \">\", " +
                        "\"activationThreshold\": 105, \"activationPersistence\": 0, " +
                        "\"resolutionThreshold\": 95, \"resolutionPersistence\": 0}");
                r.setSeverity(com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO);
                updatedAny = true;
            } else if (dsl != null && !dsl.contains("resolutionThreshold")) {
                if (r.getName() != null && r.getName().contains("Taquipneia")) {
                    r.setExpressionDsl("{\"metric\": \"RR\", \"operator\": \">\", " +
                            "\"activationThreshold\": 30, \"activationPersistence\": 20, " +
                            "\"resolutionThreshold\": 25, \"resolutionPersistence\": 20}");
                    updatedAny = true;
                } else if (r.getName() != null && r.getName().contains("Hipoxia")) {
                    r.setExpressionDsl("{\"metric\": \"SPO2\", \"operator\": \"<\", " +
                            "\"activationThreshold\": 97, \"activationPersistence\": 40, " +
                            "\"resolutionThreshold\": 98, \"resolutionPersistence\": 40}");
                    updatedAny = true;
                } else if (r.getName() != null && r.getName().contains("Volume Corrente")) {
                    r.setExpressionDsl("{\"metric\": \"TidalVolume\", \"operator\": \"<\", " +
                            "\"activationThreshold\": 200, \"activationPersistence\": 15, " +
                            "\"resolutionThreshold\": 250, \"resolutionPersistence\": 15}");
                    updatedAny = true;
                } else if (r.getName() != null && r.getName().contains("Acidose")) {
                    r.setExpressionDsl("{\"metric\": \"ArterialBloodPH\", \"operator\": \"<\", " +
                            "\"activationThreshold\": 7.35, \"activationPersistence\": 60, " +
                            "\"resolutionThreshold\": 7.38, \"resolutionPersistence\": 60}");
                    updatedAny = true;
                }
            }
        }
        if (updatedAny) {
            ruleRepository.saveAll(allRules);
            System.out.println("[SEEDER] Atualizadas Regras Clínicas antigas com os parâmetros de Histerese!");
        }

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
                    .name("Taquicardia Severa")
                    .expressionDsl("{\"metric\": \"HEART_RATE\", \"operator\": \">\", \"activationThreshold\": 105, " +
                                   "\"activationPersistence\": 0, \"resolutionThreshold\": 95, \"resolutionPersistence\": 0}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO)
                    .analyticalJustification("Aumento sustentado da frequência cardíaca indica possível choque compensado ou stress fisiológico agudo.")
                    .system(cardio).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();
                
                // 2. Taquipneia
                com.grupo3aor.innovationlab.domain.entity.Rule rrRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Taquipneia")
                    .expressionDsl("{\"metric\": \"RR\", \"operator\": \">\", \"activationThreshold\": 30, " +
                                   "\"activationPersistence\": 20, \"resolutionThreshold\": 25, \"resolutionPersistence\": 20}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO)
                    .analyticalJustification("Frequência respiratória elevada sugere dificuldade respiratória iminente ou acidose metabólica compensatória.")
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                // 3. Hipoxia
                com.grupo3aor.innovationlab.domain.entity.Rule spo2Rule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Hipoxia Crítica")
                    .expressionDsl("{\"metric\": \"SPO2\", \"operator\": \"<\", \"activationThreshold\": 97, " +
                                   "\"activationPersistence\": 40, \"resolutionThreshold\": 98, \"resolutionPersistence\": 40}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.ALERTA)
                    .analyticalJustification("A queda de saturação de oxigénio reflete trocas gasosas inadequadas a nível alveolar.")
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                // 4. Volume Corrente Baixo (Broncoconstrição)
                com.grupo3aor.innovationlab.domain.entity.Rule tidalRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Broncoconstrição / Hipoventilação")
                    .expressionDsl("{\"metric\": \"TidalVolume\", \"operator\": \"<\", \"activationThreshold\": 200, " +
                                   "\"activationPersistence\": 15, \"resolutionThreshold\": 250, \"resolutionPersistence\": 15}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.ALERTA)
                    .analyticalJustification("Volume corrente abaixo do limiar aponta para obstrução das vias aéreas inferiores ou fraqueza muscular.")
                    .system(resp).active(true).deleted(false).createdBy("system_seeder").updatedBy("system_seeder").build();

                // 5. Acidose Respiratória
                com.grupo3aor.innovationlab.domain.entity.Rule phRule = com.grupo3aor.innovationlab.domain.entity.Rule.builder()
                    .name("Acidose Respiratória")
                    .expressionDsl("{\"metric\": \"ArterialBloodPH\", \"operator\": \"<\", \"activationThreshold\": 7.35, " +
                                   "\"activationPersistence\": 60, \"resolutionThreshold\": 7.38, \"resolutionPersistence\": 60}")
                    .severity(com.grupo3aor.innovationlab.domain.enums.Severity.CRITICO)
                    .analyticalJustification("Acumulação de CO2 no sangue devido a ventilação alveolar inadequada.")
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
                .firstName("Admin")
                .lastName("Admin")
                .email("admin@gmail.com")
                .passwordHash(passwordEncoder.encode("Pass1234#"))
                .perfil(com.grupo3aor.innovationlab.domain.enums.PerfilEnum.ADMIN)
                .accountActivated(true)
                .active(true)
                .createdBy("system_seeder")
                .updatedBy("system_seeder")
                .build();

            userRepository.save(luis);
            System.out.println("[SEEDER] Utilizador admin@gmail.com criado com sucesso!");
        }
    }
}
