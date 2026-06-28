package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.Severity;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZZZEntityCoverageTest {

    @Test
    @DisplayName("Alert equals and hashCode")
    void testAlertEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Alert alert1 = new Alert();
        alert1.setId(id1);

        Alert alert2 = new Alert();
        alert2.setId(id1);

        Alert alert3 = new Alert();
        alert3.setId(id2);

        assertThat(alert1).isEqualTo(alert1);
        assertThat(alert1).isEqualTo(alert2);
        assertThat(alert1).isNotEqualTo(alert3);
        assertThat(alert1).isNotEqualTo(null);
        assertThat(alert1).isNotEqualTo(new Object());
        assertThat(alert1.hashCode()).isEqualTo(alert2.hashCode());
    }

    @Test
    @DisplayName("PhysiologicalReading equals and hashCode")
    void testPhysiologicalReadingEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        PhysiologicalReading reading1 = new PhysiologicalReading();
        reading1.setId(id1);

        PhysiologicalReading reading2 = new PhysiologicalReading();
        reading2.setId(id1);

        PhysiologicalReading reading3 = new PhysiologicalReading();
        reading3.setId(id2);

        assertThat(reading1).isEqualTo(reading1);
        assertThat(reading1).isEqualTo(reading2);
        assertThat(reading1).isNotEqualTo(reading3);
        assertThat(reading1).isNotEqualTo(null);
        assertThat(reading1).isNotEqualTo(new Object());
        assertThat(reading1.hashCode()).isEqualTo(reading2.hashCode());
    }

    @Test
    @DisplayName("Rule equals and hashCode")
    void testRuleEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Rule rule1 = new Rule();
        rule1.setId(id1);

        Rule rule2 = new Rule();
        rule2.setId(id1);

        Rule rule3 = new Rule();
        rule3.setId(id2);

        assertThat(rule1).isEqualTo(rule1);
        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1).isNotEqualTo(rule3);
        assertThat(rule1).isNotEqualTo(null);
        assertThat(rule1).isNotEqualTo(new Object());
        assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
    }

    @Test
    @DisplayName("Rule isTriggeredBy logic")
    void testRuleIsTriggeredBy() {
        Rule rule = new Rule();
        
        // Empty DSL
        assertThat(rule.isTriggeredBy("HR", 100.0)).isFalse();
        
        // Invalid DSL
        rule.setExpressionDsl("invalid-dsl");
        assertThat(rule.isTriggeredBy("HR", 100.0)).isFalse();
        
        // HEART_RATE > 100
        rule.setExpressionDsl("{\"metric\":\"HEART_RATE\",\"operator\":\">\",\"activationThreshold\":100.0}");
        assertThat(rule.isTriggeredBy("HR", 120.0)).isTrue();
        assertThat(rule.isTriggeredBy("HR", 90.0)).isFalse();
        
        // SPO2 < 90
        rule = new Rule(); // reset cache
        rule.setExpressionDsl("{\"metric\":\"SPO2\",\"operator\":\"<\",\"activationThreshold\":90.0}");
        assertThat(rule.isTriggeredBy("SpO2", 85.0)).isTrue();
        assertThat(rule.isTriggeredBy("SpO2", 95.0)).isFalse();
        
        // BP == 120
        rule = new Rule();
        rule.setExpressionDsl("{\"metric\":\"BP\",\"operator\":\"==\",\"activationThreshold\":120.0}");
        assertThat(rule.isTriggeredBy("SBP", 120.0)).isTrue();
        assertThat(rule.isTriggeredBy("SBP", 130.0)).isFalse();

        // RR > 20
        rule = new Rule();
        rule.setExpressionDsl("{\"metric\":\"RR\",\"operator\":\">\",\"activationThreshold\":20.0}");
        assertThat(rule.isTriggeredBy("RespirationRate", 25.0)).isTrue();
        assertThat(rule.isTriggeredBy("RespirationRate", 15.0)).isFalse();

        // TEMP > 38
        rule = new Rule();
        rule.setExpressionDsl("{\"metric\":\"TEMP\",\"operator\":\">\",\"activationThreshold\":38.0}");
        assertThat(rule.isTriggeredBy("CoreTemperature", 39.0)).isTrue();
        assertThat(rule.isTriggeredBy("CoreTemperature", 37.0)).isFalse();

        // Custom metric > 10
        rule = new Rule();
        rule.setExpressionDsl("{\"metric\":\"CustomMetric\",\"operator\":\">\",\"activationThreshold\":10.0}");
        assertThat(rule.isTriggeredBy("CustomMetric", 15.0)).isTrue();
        assertThat(rule.isTriggeredBy("WrongMetric", 15.0)).isFalse();
        
        // Invalid operator
        rule = new Rule();
        rule.setExpressionDsl("{\"metric\":\"HR\",\"operator\":\"!=\",\"activationThreshold\":100.0}");
        assertThat(rule.isTriggeredBy("HR", 150.0)).isFalse();
    }

    @Test
    @DisplayName("Rule getActivationPersistence logic")
    void testRuleGetActivationPersistence() {
        Rule rule = new Rule();
        
        // Empty DSL
        assertThat(rule.getActivationPersistence()).isEqualTo(0);
        
        // Invalid DSL
        rule.setExpressionDsl("invalid-dsl");
        assertThat(rule.getActivationPersistence()).isEqualTo(0);
        
        // Valid DSL with activation persistence
        rule.setExpressionDsl("{\"metric\":\"HEART_RATE\",\"operator\":\">\",\"activationThreshold\":100,\"activationPersistence\":30}");
        assertThat(rule.getActivationPersistence()).isEqualTo(30);

        // No persistence mapped should return 0 safely
        rule.setExpressionDsl("{\"metric\":\"HEART_RATE\",\"operator\":\">\",\"activationThreshold\":100}");
        assertThat(rule.getActivationPersistence()).isEqualTo(0);

        rule.setExpressionDsl("invalid-json");
        // Invalid JSON should return 0 safely
        assertThat(rule.getActivationPersistence()).isEqualTo(0);

        rule.setExpressionDsl(null);
        assertThat(rule.getActivationPersistence()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Simulation equals and hashCode")
    void testSimulationEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Simulation sim1 = new Simulation();
        sim1.setId(id1);

        Simulation sim2 = new Simulation();
        sim2.setId(id1);

        Simulation sim3 = new Simulation();
        sim3.setId(id2);

        assertThat(sim1).isEqualTo(sim1);
        assertThat(sim1).isEqualTo(sim2);
        assertThat(sim1).isNotEqualTo(sim3);
        assertThat(sim1).isNotEqualTo(null);
        assertThat(sim1).isNotEqualTo(new Object());
        assertThat(sim1.hashCode()).isEqualTo(sim2.hashCode());
    }

    @Test
    @DisplayName("ClinicalScenario equals and hashCode")
    void testClinicalScenarioEqualsAndHashCode() {
        Long id1 = 1L;
        Long id2 = 2L;

        ClinicalScenario cs1 = new ClinicalScenario();
        cs1.setId(id1);

        ClinicalScenario cs2 = new ClinicalScenario();
        cs2.setId(id1);

        ClinicalScenario cs3 = new ClinicalScenario();
        cs3.setId(id2);

        assertThat(cs1).isEqualTo(cs1);
        assertThat(cs1).isEqualTo(cs2);
        assertThat(cs1).isNotEqualTo(cs3);
        assertThat(cs1).isNotEqualTo(null);
        assertThat(cs1).isNotEqualTo(new Object());
        assertThat(cs1.hashCode()).isEqualTo(cs2.hashCode());
    }
    
    @Test
    @DisplayName("PhysiologicalSystem equals and hashCode")
    void testPhysiologicalSystemEqualsAndHashCode() {
        Long id1 = 1L;
        Long id2 = 2L;

        PhysiologicalSystem sys1 = new PhysiologicalSystem();
        sys1.setId(id1);

        PhysiologicalSystem sys2 = new PhysiologicalSystem();
        sys2.setId(id1);

        PhysiologicalSystem sys3 = new PhysiologicalSystem();
        sys3.setId(id2);

        assertThat(sys1).isEqualTo(sys1);
        assertThat(sys1).isEqualTo(sys2);
        assertThat(sys1).isNotEqualTo(sys3);
        assertThat(sys1).isNotEqualTo(null);
        assertThat(sys1).isNotEqualTo(new Object());
        assertThat(sys1.hashCode()).isEqualTo(sys2.hashCode());
    }

    @Test
    @DisplayName("EvaluationReport equals and hashCode")
    void testEvaluationReportEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        EvaluationReport rep1 = new EvaluationReport();
        rep1.setId(id1);

        EvaluationReport rep2 = new EvaluationReport();
        rep2.setId(id1);

        EvaluationReport rep3 = new EvaluationReport();
        rep3.setId(id2);

        assertThat(rep1).isEqualTo(rep1);
        assertThat(rep1).isEqualTo(rep2);
        assertThat(rep1).isNotEqualTo(rep3);
        assertThat(rep1).isNotEqualTo(null);
        assertThat(rep1).isNotEqualTo(new Object());
        assertThat(rep1.hashCode()).isEqualTo(rep2.hashCode());
    }

    @Test
    @DisplayName("User equals and hashCode")
    void testUserEqualsAndHashCode() {
        Long id1 = 1L;
        Long id2 = 2L;

        User u1 = new User();
        u1.setId(id1);

        User u2 = new User();
        u2.setId(id1);

        User u3 = new User();
        u3.setId(id2);

        assertThat(u1).isEqualTo(u1);
        assertThat(u1).isEqualTo(u2);
        assertThat(u1).isNotEqualTo(u3);
        assertThat(u1).isNotEqualTo(null);
        assertThat(u1).isNotEqualTo(new Object());
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }
}
