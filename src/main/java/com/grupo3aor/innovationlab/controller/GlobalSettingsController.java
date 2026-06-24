package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.GlobalSettings;
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public GlobalSettings getSettings() {
        return service.getSettings();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public GlobalSettings updateSettings(@RequestBody GlobalSettings settings) {
        return service.updateSettings(settings);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteSettings(@RequestBody GlobalSettings settings) {
        service.deleteSettings(settings);
    }
       
}
    