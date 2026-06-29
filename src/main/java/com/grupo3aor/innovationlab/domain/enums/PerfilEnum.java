package com.grupo3aor.innovationlab.domain.enums;

/**
 * I decided to define the three distinct user profile roles operating within our system.
 * <p>
 * - ADMIN:   Our global Administrator. I assigned this role to manage users and system configurations.
 * - MANAGER: The laboratory Manager. I gave this role clearance to create simulation rules and analyze alerts.
 * - USER:    The standard laboratory User. I restricted this role to executing simulations and viewing personal data.
 * </p>
 * NOTE: Further down the pipeline in the User entity, I mapped this enum using the @Enumerated(EnumType.STRING) annotation.
 * I engineered it this way to ensure database records remain highly readable during audits, avoiding obscure numerical indices.
 *
 */
public enum PerfilEnum {
    ADMIN,
    MANAGER,
    USER
}
