package com.grupo3aor.innovationlab.bootstrap;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.repository.PhysiologicalSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Automatically seeds the database with essential physiological systems if they don't exist yet.
 */
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final PhysiologicalSystemRepository physiologicalSystemRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedPhysiologicalSystems();
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
}
