package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.GlobalSettingsDTO;
import com.grupo3aor.innovationlab.service.GlobalSettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class GlobalSettingsController {

    private final GlobalSettingsService service;

    public GlobalSettingsController(GlobalSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public GlobalSettingsDTO getSettings() {
        return service.getSettings();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public GlobalSettingsDTO updateSettings(@RequestBody GlobalSettingsDTO settings) {
        return service.updateSettings(settings);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteSettings(@RequestBody(required = false) GlobalSettingsDTO settings) {
        service.deleteSettings(settings);
    }
       
}