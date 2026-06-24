package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.GlobalSettings;
import com.grupo3aor.innovationlab.repository.GlobalSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GlobalSettingsService {

    @Autowired
    private GlobalSettingsRepository repository;

    public GlobalSettings getSettings() {
        GlobalSettings dataRepository = repository.getSettings();
        if (dataRepository == null) {
            //create new default data
            dataRepository = new GlobalSettings();
            dataRepository.setId(1L);
            dataRepository.setSessionTimeoutMinutes(30);
            dataRepository.setIsHumanBodyEnabled(false);
            return repository.save(dataRepository);
        }
        return dataRepository;
    }

    public GlobalSettings updateSettings(GlobalSettings settings) {
        return repository.save(settings);
    }

    public void deleteSettings(GlobalSettings settings) {
        repository.delete(settings);
    }
       
}