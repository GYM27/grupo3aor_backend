package com.grupo3aor.innovationlab.domain.enums;

/**
 * I created this enum to track the lifecycle of a clinical simulation execution.
 * <p>
 * Using an Enum ensures I don't accidentally insert typos into the database
 * when changing the status of an ongoing simulation.
 * </p>
 */
public enum SimulationStatus {
    INICIADA,
    EM_CURSO,
    PAUSADA,
    FINALIZADA,
    CANCELADA
}
