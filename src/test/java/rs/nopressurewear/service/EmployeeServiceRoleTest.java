package rs.nopressurewear.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.dto.user.UserRequest;
import rs.nopressurewear.dto.user.UserResponse;
import rs.nopressurewear.dto.user.UserUpdateRequest;
import rs.nopressurewear.exception.ConflictException;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers {@code backend-employee-role-edit.md} section 7's test table: role changes now go
 * through {@code update(...)} (no more standalone role endpoint), SUPER_ADMIN is assignable
 * to an existing account by another SUPER_ADMIN, and the "at least one active SUPER_ADMIN"
 * invariant is enforced on update/delete/deactivate.
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class EmployeeServiceRoleTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User persist(String email, Role role) {
        return persist(email, role, true);
    }

    private User persist(String email, Role role, boolean active) {
        return userRepository.save(User.builder()
                .firstName("T").lastName("U").email(email).password("x").role(role).isActive(active).build());
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private UserRequest createRequest(String email, Role role) {
        UserRequest r = new UserRequest();
        r.setFirstName("New");
        r.setLastName("Hire");
        r.setEmail(email);
        r.setPassword("Passw0rd!");
        r.setRole(role);
        return r;
    }

    private UserUpdateRequest updateRequest(String firstName, String lastName, String email, Role role) {
        UserUpdateRequest r = new UserUpdateRequest();
        r.setFirstName(firstName);
        r.setLastName(lastName);
        r.setEmail(email);
        r.setRole(role);
        return r;
    }

    // ---------- create: still EMPLOYEE/ADMIN only (decision D) ----------

    @Test
    void create_honoursRequestedRole_forEmployeeAndAdmin() {
        authenticateAs(persist("super1@test.com", Role.SUPER_ADMIN));

        assertThat(employeeService.create(createRequest("emp1@test.com", Role.EMPLOYEE)).getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(employeeService.create(createRequest("adm1@test.com", Role.ADMIN)).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void create_rejectsSuperAdminRole() {
        authenticateAs(persist("super2@test.com", Role.SUPER_ADMIN));

        assertThatThrownBy(() -> employeeService.create(createRequest("x@test.com", Role.SUPER_ADMIN)))
                .isInstanceOf(FieldValidationException.class);
    }

    // ---------- update: role change through the ordinary update endpoint ----------

    @Test
    void update_promotesEmployeeToAdmin_bumpsTokenVersion() {
        authenticateAs(persist("super3@test.com", Role.SUPER_ADMIN));
        User emp = persist("emp3@test.com", Role.EMPLOYEE);
        int versionBefore = emp.getTokenVersion();

        UserResponse updated = employeeService.update(emp.getId(),
                updateRequest("Em", "Ployee", "emp3@test.com", Role.ADMIN));

        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
        User reloaded = userRepository.findById(emp.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.ADMIN);
        assertThat(reloaded.getTokenVersion()).isEqualTo(versionBefore + 1);
    }

    @Test
    void update_grantsSuperAdminToExistingAccount() {
        authenticateAs(persist("super4@test.com", Role.SUPER_ADMIN));
        User emp = persist("emp4@test.com", Role.EMPLOYEE);

        UserResponse updated = employeeService.update(emp.getId(),
                updateRequest("Em", "Ployee", "emp4@test.com", Role.SUPER_ADMIN));

        assertThat(updated.getRole()).isEqualTo(Role.SUPER_ADMIN);
    }

    @Test
    void update_rejectsUnassignableRole() {
        authenticateAs(persist("super5@test.com", Role.SUPER_ADMIN));
        User emp = persist("emp5@test.com", Role.EMPLOYEE);

        assertThatThrownBy(() -> employeeService.update(emp.getId(),
                updateRequest("Em", "Ployee", "emp5@test.com", Role.CUSTOMER)))
                .isInstanceOf(FieldValidationException.class);
    }

    @Test
    void update_onOwnAccount_changingRole_isRejected() {
        User me = persist("super6@test.com", Role.SUPER_ADMIN);
        authenticateAs(me);

        assertThatThrownBy(() -> employeeService.update(me.getId(),
                updateRequest("Me", "Myself", "super6@test.com", Role.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_onOwnAccount_nameOnlyNoRoleChange_succeeds() {
        User me = persist("super7@test.com", Role.SUPER_ADMIN);
        authenticateAs(me);
        int versionBefore = me.getTokenVersion();

        // role equals current role -> not a role change, name/email edit goes through
        UserResponse updated = employeeService.update(me.getId(),
                updateRequest("Renamed", "Self", "super7@test.com", Role.SUPER_ADMIN));

        assertThat(updated.getFirstName()).isEqualTo("Renamed");
        assertThat(updated.getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(userRepository.findById(me.getId())).get()
                .satisfies(u -> assertThat(u.getTokenVersion()).isEqualTo(versionBefore)); // unchanged: no role change
    }

    @Test
    void update_demotesASuperAdmin_whenAnotherActiveOneRemains() {
        User actor = persist("super8@test.com", Role.SUPER_ADMIN);
        authenticateAs(actor);
        User target = persist("super8b@test.com", Role.SUPER_ADMIN);

        UserResponse updated = employeeService.update(target.getId(),
                updateRequest("Target", "Super", "super8b@test.com", Role.ADMIN));

        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void update_demotingTheOnlyActiveSuperAdmin_isRejected() {
        // The caller must itself hold SUPER_ADMIN to reach this code path at all; the only way
        // to exercise "target is the last *active* SUPER_ADMIN" without tripping the self-change
        // guard is a caller whose own account is SUPER_ADMIN but inactive (e.g. deactivated after
        // the token was issued) — deliberately not counted toward the invariant either.
        User inactiveActor = persist("super9@test.com", Role.SUPER_ADMIN, false);
        authenticateAs(inactiveActor);
        User onlyActive = persist("super9b@test.com", Role.SUPER_ADMIN, true);

        assertThatThrownBy(() -> employeeService.update(onlyActive.getId(),
                updateRequest("Only", "Active", "super9b@test.com", Role.ADMIN)))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- delete / deactivate: last-active-SUPER_ADMIN invariant + self-block ----------

    @Test
    void delete_ownAccount_isRejected() {
        User me = persist("super10@test.com", Role.SUPER_ADMIN);
        authenticateAs(me);

        assertThatThrownBy(() -> employeeService.delete(me.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_theOnlyActiveSuperAdmin_isRejected() {
        User inactiveActor = persist("super11@test.com", Role.SUPER_ADMIN, false);
        authenticateAs(inactiveActor);
        User onlyActive = persist("super11b@test.com", Role.SUPER_ADMIN, true);

        assertThatThrownBy(() -> employeeService.delete(onlyActive.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivate_ownAccount_isRejected() {
        User me = persist("super12@test.com", Role.SUPER_ADMIN);
        authenticateAs(me);

        assertThatThrownBy(() -> employeeService.deactivate(me.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deactivate_theOnlyActiveSuperAdmin_isRejected() {
        User inactiveActor = persist("super13@test.com", Role.SUPER_ADMIN, false);
        authenticateAs(inactiveActor);
        User onlyActive = persist("super13b@test.com", Role.SUPER_ADMIN, true);

        assertThatThrownBy(() -> employeeService.deactivate(onlyActive.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivatedSuperAdmin_doesNotCountTowardTheInvariant() {
        // Two SUPER_ADMIN rows exist, but only one is active -> demoting/deleting/deactivating
        // that one active row must still be refused, because the deactivated row doesn't count.
        long activeCount = userRepository.countByRoleAndIsActiveTrueAndIdNot(Role.SUPER_ADMIN, -1L);

        User inactive = persist("super14@test.com", Role.SUPER_ADMIN, false);
        User active = persist("super14b@test.com", Role.SUPER_ADMIN, true);

        long remainingExcludingActive = userRepository.countByRoleAndIsActiveTrueAndIdNot(Role.SUPER_ADMIN, active.getId());
        assertThat(remainingExcludingActive).isEqualTo(activeCount); // the inactive row contributed nothing
    }

    // ---------- listing ----------

    @Test
    void listing_includesAllThreeStaffTiers() {
        authenticateAs(persist("super15@test.com", Role.SUPER_ADMIN));
        persist("emp15@test.com", Role.EMPLOYEE);
        persist("adm15@test.com", Role.ADMIN);
        persist("cust15@test.com", Role.CUSTOMER);

        var roles = employeeService.getAll(PageRequest.of(0, 50))
                .map(UserResponse::getRole).getContent();

        assertThat(roles).contains(Role.EMPLOYEE, Role.ADMIN, Role.SUPER_ADMIN).doesNotContain(Role.CUSTOMER);
    }
}
