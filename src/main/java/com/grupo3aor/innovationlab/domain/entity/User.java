package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing the "users" table in the database.
 *
 * IMPLEMENTATION NOTES:
 * - @Entity, @Table, @Id and @Column are standard JPA annotations.
 * - Utilizing Lombok annotations (@Data, @Builder) to reduce boilerplate code (getters, setters, constructors).
 */
@Entity
@Table(name = "users") // Pluralized to avoid SQL reserved keyword conflicts
@Getter             // Granular Lombok annotations instead of @Data
@Setter             // to prevent StackOverflow and performance issues
@ToString           // when introducing relationships (e.g. @OneToMany) later on.
@NoArgsConstructor  // Required by JPA
@AllArgsConstructor // Required by @Builder
@Builder            // Enables Builder pattern: User.builder().firstName("Ana").build()
public class User {

    // =========================================================
    // PRIMARY KEY
    // Using IDENTITY for auto-increment.
    // =========================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // PERSONAL DATA
    // @NotBlank ensures mandatory fields are not null or empty.
    // @Size adds a security constraint on the maximum length in the DB.
    // =========================================================
    @NotBlank(message = "First name cannot be empty")
    @Size(max = 75, message = "First name cannot exceed 75 characters")
    @Column(nullable = false, length = 75)
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(max = 75, message = "Last name cannot exceed 75 characters")
    @Column(nullable = false, length = 75)
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    @Column(nullable = false, unique = true) // Enforce unique emails in the database
    private String email;

    // CRITICAL: Storing only the BCrypt hash of the password, never plain text.
    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    // =========================================================
    // PROFILE
    // EnumType.STRING saves "ADMIN" instead of numeric indices for readability.
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilEnum perfil;

    // =========================================================
    // SOFT DELETE
    // Disabling user instead of deleting to keep historical data.
    // =========================================================
    @Column(nullable = false)
    @Builder.Default // Respect the default value when using @Builder
    private boolean ativo = true; // By default, new users are active

    // =========================================================
    // EMAIL ACTIVATION LOGIC
    // Users must activate their accounts via email link before logging in.
    // =========================================================
    @Builder.Default
    private boolean ativado = false; // New users start as not activated

    private String activationToken; // The token sent via email

    // =========================================================
    // AUTOMATIC AUDITING
    // Hibernate handles these timestamps automatically.
    // =========================================================
    @CreationTimestamp
    @Column(nullable = false, updatable = false) // Cannot be updated after creation
    private LocalDateTime criadoEm;

    @UpdateTimestamp // Automatically updated on modifications
    private LocalDateTime atualizadoEm;

    // =========================================================
    // ENTITY IDENTITY (EQUALS & HASHCODE)
    // Equals based purely on ID to guarantee collection stability and prevent 
    // circular references in JPA relationships.
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        // Two users are equal if they share the same non-null ID.
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Fixed value to ensure hashCode stability between instantiation and DB persistence.
        return getClass().hashCode();
    }
}
