package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.GlobalSettings;
import com.grupo3aor.innovationlab.dto.GlobalSettingsDTO;
import com.grupo3aor.innovationlab.repository.GlobalSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for managing global application settings.
 */
@Service
public class GlobalSettingsService {

    @Autowired
    private GlobalSettingsRepository repository;

    private final AtomicBoolean isDbFailed = new AtomicBoolean(false);

    public void setDbFailed(boolean status) {
        this.isDbFailed.set(status);
    }

    public boolean isDbFailed() {
        return this.isDbFailed.get();
    }

    public GlobalSettingsDTO getSettings() {
        GlobalSettings dataRepository = repository.getSettings();
        if (dataRepository == null) {
            //create new default data
            dataRepository = new GlobalSettings();
            dataRepository.setId(1L);
            dataRepository.setSessionTimeoutMinutes(30);
            dataRepository.setIsHumanBodyEnabled(false);
            dataRepository = repository.save(dataRepository);
        }
        return mapToDTO(dataRepository);
    }

    public GlobalSettingsDTO updateSettings(GlobalSettingsDTO settingsDto) {
        GlobalSettings dataRepository = repository.getSettings();
        if (dataRepository == null) {
            dataRepository = new GlobalSettings();
            dataRepository.setId(1L);
        }
        
        if (settingsDto.getSessionTimeoutMinutes() != null) {
            dataRepository.setSessionTimeoutMinutes(settingsDto.getSessionTimeoutMinutes());
        }
        if (settingsDto.getIsHumanBodyEnabled() != null) {
            dataRepository.setIsHumanBodyEnabled(settingsDto.getIsHumanBodyEnabled());
        }
        if (settingsDto.getIsDbFailed() != null) {
            setDbFailed(settingsDto.getIsDbFailed());
        }
        
        dataRepository = repository.save(dataRepository);
        return mapToDTO(dataRepository);
    }

    public void deleteSettings(GlobalSettingsDTO settingsDto) {
        GlobalSettings dataRepository = repository.getSettings();
        if (dataRepository != null) {
            repository.delete(dataRepository);
        }
    }
    
    private GlobalSettingsDTO mapToDTO(GlobalSettings settings) {
        return GlobalSettingsDTO.builder()
                .sessionTimeoutMinutes(settings.getSessionTimeoutMinutes())
                .isHumanBodyEnabled(settings.getIsHumanBodyEnabled())
                .isDbFailed(this.isDbFailed.get())
                .build();
    }
}