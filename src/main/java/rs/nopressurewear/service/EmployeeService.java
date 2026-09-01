package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.dto.user.UserRequest;
import rs.nopressurewear.dto.user.UserResponse;
import rs.nopressurewear.dto.user.UserUpdateRequest;
import rs.nopressurewear.exception.ConflictException;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.repository.UserRepository;
import rs.nopressurewear.security.AuthUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static java.util.Objects.nonNull;
import static rs.nopressurewear.constants.Role.ADMIN;
import static rs.nopressurewear.constants.Role.EMPLOYEE;
import static rs.nopressurewear.constants.Role.SUPER_ADMIN;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    /** Roles a brand-new account may be created with. SUPER_ADMIN is handed over, never created (decision D). */
    private static final Set<Role> CREATABLE_ROLES = Set.of(EMPLOYEE, ADMIN);
    /** Roles an existing staff account may be moved to via update — SUPER_ADMIN included, for handover. */
    private static final Set<Role> UPDATE_ASSIGNABLE_ROLES = Set.of(EMPLOYEE, ADMIN, SUPER_ADMIN);
    /** Roles this service manages/lists. */
    private static final List<Role> STAFF_ROLES = List.of(EMPLOYEE, ADMIN, SUPER_ADMIN);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuthUtil authUtil;
    private final EmailService emailService;
    private final StoreSettingsRepository settingsRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("validation.emailTaken", "email");
        }
        if (request.getRole() == null || !CREATABLE_ROLES.contains(request.getRole())) {
            throw new FieldValidationException("Only EMPLOYEE and ADMIN can be assigned", "role");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    /**
     * Updates a staff account's profile and, optionally, its role — in one transaction, so a
     * name change can never succeed while a bundled role change silently fails. Validation
     * order (fail-fast): target exists, self-role-change is refused, the requested role is one
     * we accept, then the last-active-SUPER_ADMIN invariant. A role change also bumps
     * {@code tokenVersion} (any token issued before this stops validating) and is logged at WARN;
     * granting SUPER_ADMIN additionally emails every other active SUPER_ADMIN.
     */
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse update(Long id, UserUpdateRequest request) {
        User target = requireStaff(id);

        Role newRole = request.getRole();
        boolean roleChanging = nonNull(newRole) && newRole != target.getRole();

        if (roleChanging) {
            if (target.getId().equals(authUtil.getCurrentUserId())) {
                throw new AccessDeniedException("You cannot change your own role");
            }
            if (!UPDATE_ASSIGNABLE_ROLES.contains(newRole)) {
                throw new FieldValidationException("Unknown role", "role");
            }
            assertNotLastActiveSuperAdmin(target);
        }

        userService.setUserData(request, target);

        Role oldRole = target.getRole();
        if (roleChanging) {
            target.setRole(newRole);
            target.setTokenVersion(target.getTokenVersion() + 1);
        }

        User saved = userRepository.save(target);

        if (roleChanging) {
            String actorEmail = authUtil.getCurrentUser().getEmail();
            log.warn("ROLE_CHANGE actor={} target={} from={} to={}", actorEmail, saved.getEmail(), oldRole, newRole);
            if (newRole == SUPER_ADMIN) {
                notifySuperAdminsOfGrant(saved, actorEmail);
            }
        }

        return toResponse(saved);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findByRoleIn(STAFF_ROLES, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<UserResponse> search(String query, Boolean active, Pageable pageable) {
        return userRepository.findStaffByFilters(query, active, pageable)
                .map(this::toResponse);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(Long id) {
        User user = requireStaff(id);
        requireNotSelf(user, "delete your own account");
        assertNotLastActiveSuperAdmin(user);
        userRepository.delete(user);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse deactivate(Long id) {
        User user = requireStaff(id);
        requireNotSelf(user, "deactivate your own account");
        assertNotLastActiveSuperAdmin(user);
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse activate(Long id) {
        User user = requireStaff(id);
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    /** Loads a staff account (EMPLOYEE, ADMIN or SUPER_ADMIN) — never a customer's row. */
    private User requireStaff(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (!STAFF_ROLES.contains(user.getRole())) {
            throw new FieldValidationException("User is not a staff member", "id");
        }
        return user;
    }

    private void requireNotSelf(User target, String action) {
        if (target.getId().equals(authUtil.getCurrentUserId())) {
            throw new AccessDeniedException("You cannot " + action);
        }
    }

    /** At least one active SUPER_ADMIN besides {@code target} must remain once this action lands. */
    private void assertNotLastActiveSuperAdmin(User target) {
        if (target.getRole() != SUPER_ADMIN) return;
        long remaining = userRepository.countByRoleAndIsActiveTrueAndIdNot(SUPER_ADMIN, target.getId());
        if (remaining == 0) {
            throw new ConflictException("There must always be at least one active SUPER_ADMIN.");
        }
    }

    /** The only defense against a quiet privilege escalation: every other active SUPER_ADMIN gets an email. */
    private void notifySuperAdminsOfGrant(User target, String actorEmail) {
        List<User> activeSuperAdmins = userRepository.findByRoleAndIsActiveTrue(SUPER_ADMIN.name());
        LocalDateTime when = LocalDateTime.now();
        String lang = defaultLanguage();
        for (User admin : activeSuperAdmins) {
            try {
                emailService.sendSuperAdminRoleGrantAlert(admin.getEmail(), target.getEmail(), actorEmail, when, lang);
            } catch (Exception e) {
                log.warn("Failed to notify {} of SUPER_ADMIN grant for {}: {}", admin.getEmail(), target.getEmail(), e.getMessage());
            }
        }
    }

    private String defaultLanguage() {
        return settingsRepository.findByKey("default_language")
                .map(s -> s.getValue())
                .orElse("en");
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
