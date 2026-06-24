package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.GlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, Long> {

    default GlobalSettings getSettings() {
        return findById(1L).orElseGet(() -> {
            GlobalSettings settings = new GlobalSettings();
            settings.setId(1L);
            settings.setSessionTimeoutMinutes(30);
            settings.setIsBioGearsEnabled(false);
            settings.setCreatedBy("system");
            settings.setUpdatedBy("system");
            return save(settings);
        });
    }
    

}