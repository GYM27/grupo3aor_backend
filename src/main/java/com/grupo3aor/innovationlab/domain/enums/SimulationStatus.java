package com.grupo3aor.innovationlab.domain.enums;

/**
 * Tracks the lifecycle of a clinical simulation execution.
 * <p>
 * Using an Enum ensures we don't accidentally insert typos into the database
 * when changing the status of an ongoing simulation.
 * </p>
 */
public enum SimulationStatus {
    INICIADA,
    EM_CURSO,
    FINALIZADA,
    CANCELADA
}
