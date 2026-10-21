package rs.nopressurewear.identity.internal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import rs.nopressurewear.identity.api.Role;
import rs.nopressurewear.identity.internal.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A JWT is only valid while its "tv" (token version) claim matches the account's current
 * {@code tokenVersion} — the mechanism behind "after a role change, the old token stops working"
 * (backend-employee-role-edit.md section 5 / 7).
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "dGVzdEp3dFNlY3JldEtleUZvckNJQnVpbGRzMzJCeXRlc0xvbmc=");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
    }

    private User user(long id, Role role, int tokenVersion) {
        return User.builder().id(id).email("u" + id + "@test.com").password("x").role(role)
                .isActive(true).tokenVersion(tokenVersion).build();
    }

    @Test
    void tokenIsValid_whenTokenVersionMatchesCurrentAccountState() {
        User account = user(1, Role.ADMIN, 0);

        String token = jwtUtil.generateToken(account);

        assertThat(jwtUtil.isTokenValid(token, account)).isTrue();
    }

    @Test
    void tokenBecomesInvalid_afterTheAccountsTokenVersionIsBumped() {
        User beforeChange = user(1, Role.ADMIN, 0);
        String tokenIssuedBeforeRoleChange = jwtUtil.generateToken(beforeChange);

        // simulates EmployeeService.update() bumping tokenVersion on a role change
        User afterChange = user(1, Role.SUPER_ADMIN, 1);

        assertThat(jwtUtil.isTokenValid(tokenIssuedBeforeRoleChange, afterChange)).isFalse();
    }

    @Test
    void freshlyIssuedToken_isValidAgainstTheNewAccountState() {
        User afterChange = user(1, Role.SUPER_ADMIN, 1);

        String freshToken = jwtUtil.generateToken(afterChange);

        assertThat(jwtUtil.isTokenValid(freshToken, afterChange)).isTrue();
    }

    // ---------- pwdChange claim (backend-forced-password-change.md section 3) ----------

    @Test
    void tokenCarriesPasswordChangeRequiredClaim_whenFlagIsSet() {
        User account = user(1, Role.EMPLOYEE, 0);
        account.setPasswordChangeRequired(true);

        String token = jwtUtil.generateToken(account);

        assertThat(jwtUtil.isPasswordChangeRequired(token)).isTrue();
    }

    @Test
    void tokenDoesNotCarryPasswordChangeRequiredClaim_whenFlagIsClear() {
        User account = user(1, Role.EMPLOYEE, 0);

        String token = jwtUtil.generateToken(account);

        assertThat(jwtUtil.isPasswordChangeRequired(token)).isFalse();
    }

    @Test
    void isPasswordChangeRequired_returnsFalse_forAGarbageToken() {
        assertThat(jwtUtil.isPasswordChangeRequired("not-a-real-token")).isFalse();
    }
}
