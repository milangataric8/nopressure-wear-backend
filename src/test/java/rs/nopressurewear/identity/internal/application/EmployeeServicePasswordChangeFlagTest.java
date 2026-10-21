package rs.nopressurewear.identity.internal.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.identity.api.Role;
import rs.nopressurewear.identity.api.dto.user.UserRequest;
import rs.nopressurewear.identity.api.dto.user.UserUpdateRequest;
import rs.nopressurewear.identity.internal.domain.User;
import rs.nopressurewear.identity.internal.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code backend-forced-password-change.md} section 2: who gets the forced-password-change flag
 * and who doesn't. Setting it is EmployeeService's job; enforcement itself lives in
 * {@code rs.nopressurewear.identity.internal.security.PasswordChangeRequiredFilter} (see {@code ForcedPasswordChangeTest}).
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class EmployeeServicePasswordChangeFlagTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User persist(String email, Role role) {
        return userRepository.save(User.builder()
                .firstName("T").lastName("U").email(email).password("x").role(role).isActive(true).build());
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void create_alwaysSetsThePasswordChangeRequiredFlag() {
        authenticateAs(persist("super-creator@test.com", Role.SUPER_ADMIN));

        var created = employeeService.create(newHireRequest("hire@test.com", Role.EMPLOYEE));

        User saved = userRepository.findById(created.getId()).orElseThrow();
        assertThat(saved.isPasswordChangeRequired()).isTrue();
    }

    @Test
    void update_settingAnotherStaffMembersPassword_setsTheFlag() {
        authenticateAs(persist("super-resetter@test.com", Role.SUPER_ADMIN));
        User target = persist("victim@test.com", Role.EMPLOYEE);

        UserUpdateRequest request = updateRequest("Victim", "Employee", "victim@test.com", target.getRole());
        request.setPassword("BrandNew1!");
        employeeService.update(target.getId(), request);

        User reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.isPasswordChangeRequired()).isTrue();
    }

    @Test
    void update_withoutTouchingThePassword_doesNotSetTheFlag() {
        authenticateAs(persist("super-editor@test.com", Role.SUPER_ADMIN));
        User target = persist("unaffected@test.com", Role.EMPLOYEE);

        employeeService.update(target.getId(),
                updateRequest("Renamed", "Employee", "unaffected@test.com", target.getRole()));

        User reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.isPasswordChangeRequired()).isFalse();
    }

    @Test
    void update_settingOwnPassword_doesNotSetTheFlag() {
        User self = persist("super-self@test.com", Role.SUPER_ADMIN);
        authenticateAs(self);

        UserUpdateRequest request = updateRequest("Super", "Self", "super-self@test.com", self.getRole());
        request.setPassword("BrandNew1!");
        employeeService.update(self.getId(), request);

        User reloaded = userRepository.findById(self.getId()).orElseThrow();
        assertThat(reloaded.isPasswordChangeRequired()).isFalse();
    }

    private UserRequest newHireRequest(String email, Role role) {
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
}
