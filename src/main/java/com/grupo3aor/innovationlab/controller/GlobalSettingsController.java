package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.GlobalSettings;
import com.grupo3aor.innovationlab.service.GlobalSettingsService;
import com.grupo3aor.innovationlab.service.DataPersistenceComponent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class GlobalSettingsController {

    private final GlobalSettingsService service;
    private final DataPersistenceComponent dataPersistenceComponent;

    public GlobalSettingsController(GlobalSettingsService service, DataPersistenceComponent dataPersistenceComponent) {
        this.service = service;
        this.dataPersistenceComponent = dataPersistenceComponent;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public GlobalSettings getSettings() {
        GlobalSettings settings = service.getSettings();
        // Injecting the volatile state here gives the frontend a real-time view of any ongoing chaos simulations.
        settings.setIsDbFailed(dataPersistenceComponent.isSimulateDbFailure());
        return settings;
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public GlobalSettings updateSettings(@RequestBody GlobalSettings settings) {
        // Intercepting the simulation flag right before persisting to DB. We want to update the live system without saving this destructive state permanently!
        if (settings.getIsDbFailed() != null) {
            dataPersistenceComponent.setSimulateDbFailure(settings.getIsDbFailed());
        }
        
        GlobalSettings savedSettings = service.updateSettings(settings);
        
        // Populating the transient flag back into the response payload to keep the UI in perfect sync.
        savedSettings.setIsDbFailed(dataPersistenceComponent.isSimulateDbFailure());
        return savedSettings;
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteSettings(@RequestBody GlobalSettings settings) {
        service.deleteSettings(settings);
    }
}