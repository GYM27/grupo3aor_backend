package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.ClinicalScenarioRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClinicalScenarioControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
    void testCreateClinicalScenario() throws Exception {
        ClinicalScenarioRequest request = new ClinicalScenarioRequest();
        request.setName("Shock Scenario");
        request.setDescription("Patient in hemorrhagic shock");
        request.setMdibVersion(1);
        request.setDevice("SimDevice");

        // It is crucial to include .with(csrf()) here for state-mutating requests,
        // otherwise Spring Security blocks the request with 403 Forbidden.
        mockMvc.perform(post("/api/clinical-scenarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Shock Scenario"));
    }

    @Test
    void testCreateScenarioWithoutAuthentication() throws Exception {
        ClinicalScenarioRequest request = new ClinicalScenarioRequest();
        request.setName("Unauthorized Scenario");
        
        // This request lacks authentication context, so it should be rejected.
        mockMvc.perform(post("/api/clinical-scenarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
