package com.grupo3aor.innovationlab;

import com.grupo3aor.innovationlab.service.EvaluationReportService;
import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TestReport implements CommandLineRunner {

    private final EvaluationReportService service;
    private final SimulationRepository simRepo;

    public TestReport(EvaluationReportService service, SimulationRepository simRepo) {
        this.service = service;
        this.simRepo = simRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("TESTING REPORT GENERATION...");
        try {
            UUID simId = UUID.fromString("231ff610-b6ff-4b34-a09e-bac9f5dcbe68");
            Simulation sim = simRepo.findById(simId).orElse(null);
            if (sim != null) {
                EvaluationReport r = new EvaluationReport();
                r.setIntervaloTemporal("Teste");
                java.lang.reflect.Method m = EvaluationReportService.class.getDeclaredMethod("generatePdfBytes", EvaluationReport.class, Simulation.class);
                m.setAccessible(true);
                m.invoke(service, r, sim);
                System.out.println("SUCCESS PDF!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
