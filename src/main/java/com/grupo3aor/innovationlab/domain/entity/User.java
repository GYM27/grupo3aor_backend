package com.grupo3aor.innovationlab.domain.entity;

import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;

/**
 * Persistence model mapping our user registry.
 * <p>
 * I designed this structure to isolate credentials, profile roles, and 
 * infrastructure auditing metadata securely within our relational database.
 * I refactored it to extend {@link Auditable}, eliminating duplicated audit
 * columns and ensuring a single source of truth for timestamp management.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Entity
@Table(name = "users")
@Getter            
@Setter            
@ToString          
@NoArgsConstructor  
@AllArgsConstructor 
@SuperBuilder            
// I configured this interceptor to rewrite the default physical deletion behavior.
// This ensures that whenever a delete instruction is issued, the platform alters
// the active flag instead of wiping the record, thus preserving structural history.
@SQLDelete(sql = "UPDATE users SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
public class User extends Auditable {

    /**
     * Internal autoincrement primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's first name.
     */
    // I applied a 75-character boundary restriction here to shield the database 
    // against unexpected payload sizes or field inflation vulnerabilities.
    @NotBlank(message = "First name cannot be empty")
    @Size(max = 75, message = "First name cannot exceed 75 characters")
    @Column(name = "first_name", nullable = false, length = 75)
    private String firstName;

    /**
     * User's last name.
     */
    // I enforced the exact same validation rules on the last name to keep structural
    // consistency and prevent database capacity overhead.
    @NotBlank(message = "Last name cannot be empty")
    @Size(max = 75, message = "Last name cannot exceed 75 characters")
    @Column(name = "last_name", nullable = false, length = 75)
    private String lastName;

    /**
     * Unique email address serving as the authentication username.
     */
    // I added a uniqueness constraint on this field to prevent duplicated accounts
    // and guarantee clear identity mapping during the authentication flow.
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Secure BCrypt cryptographic hash of the user's password.
     */
    // I opted to store only secure, salted cryptographic hashes here. I made sure 
    // plain-text credentials never touch our persistent storage layer.
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Security profile matching the user's explicit roles.
     */
    // I decided to persist this enum as a String representation rather than an integer index.
    // This choice makes our SQL records significantly easier to read during database audits.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilEnum perfil;

    /**
     * Soft delete operational indicator.
     */
    // I chose a primitive boolean flag to handle logical deletion safely, preventing 
    // accidental losses while keeping historical records accessible for internal tools.
    @Column(nullable = false)
    @Builder.Default 
    private boolean active = true; 

    /**
     * Verification state of the user's account.
     */
    // I initialized this state as false to force the account verification workflow,
    // ensuring users must confirm their email before accessing protected services.
    @Column(name = "account_activated", nullable = false)
    @Builder.Default
    private boolean accountActivated = false; 

    /**
     * Validation token shipped via email.
     */
    @Column(name = "activation_token")
    private String activationToken; 

    /**
     * Token used to reset the user's password securely.
     */
    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    /**
     * Expiration timestamp for the reset password token.
     */
    @Column(name = "reset_password_expires_at")
    private LocalDateTime resetPasswordExpiresAt;

    /**
     * Comparative evaluation relying solely on object primary identifiers.
     * * @param o Object compared against.
     * @return true if identities are identical; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id != null && id.equals(other.getId());
    }

    /**
     * Returns a stable object hashcode.
     * * @return Deterministic integer matching the persistent class type.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
