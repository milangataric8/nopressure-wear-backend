package rs.nopressurewear.identity.internal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rs.nopressurewear.identity.api.Role;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.util.Objects.nonNull;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails, rs.nopressurewear.platform.api.LocalizedRecipient {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = ALL, fetch = LAZY)
    private List<Address> addresses;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiry;

    @Column
    private String stripeCustomerId;

    @Column
    private String phone;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    /** Bumped whenever this account's role changes; a JWT issued before the bump stops validating. */
    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    /**
     * True whenever someone other than the account owner set its current password (SUPER_ADMIN
     * creating an employee, or resetting another staff member's password) — that person now
     * knows it too. Cleared once the owner changes it themselves via {@code /api/auth/change-password}.
     * Enforced request-by-request by {@link rs.nopressurewear.identity.internal.security.PasswordChangeRequiredFilter}.
     */
    @Builder.Default
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired = false;

    /**
     * ISO language tag ({@code "sr"}, {@code "en"}) the UI was set to when this account last
     * expressed a preference (registration, or a later language switch). Transactional emails
     * read this instead of {@code Accept-Language} — see {@code backend-message-i18n-spec.md}
     * section 4.3: an email is often sent async, or triggered by someone other than the
     * recipient, so there's no request-scoped language signal to use at send time.
     */
    @Builder.Default
    @Column(name = "preferred_locale", nullable = false)
    private String preferredLocale = "sr";

    @Transient
    public boolean isLocked() {
        return nonNull(lockUntil) && lockUntil.isAfter(LocalDateTime.now());
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return isActive; }
}
