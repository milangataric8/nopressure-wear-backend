package rs.nopressurewear.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of {@code backend-forced-password-change.md} section 4 / 7's test table:
 * a token carrying {@code pwdChange: true} is rejected everywhere except the whitelist, and a
 * successful change clears the flag, hands back a working token, and kills the old one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@Transactional
class ForcedPasswordChangeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User persistEmployee(String email, boolean passwordChangeRequired) {
        return userRepository.save(User.builder()
                .firstName("New").lastName("Hire").email(email)
                .password(passwordEncoder.encode("Temp123!"))
                .role(Role.EMPLOYEE).isActive(true)
                .passwordChangeRequired(passwordChangeRequired)
                .build());
    }

    @Test
    void tokenWithPasswordChangeRequired_isBlockedEverywhere_exceptTheWhitelist() throws Exception {
        User employee = persistEmployee("newhire1@test.com", true);
        String token = jwtUtil.generateToken(employee);

        // blocked: an endpoint this role would otherwise reach (self-access to its own profile).
        // backend-message-i18n-spec.md 3.3/3.4: ApiError shape, no literal "message" text.
        mockMvc.perform(get("/api/users/" + employee.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"))
                .andExpect(jsonPath("$.messageKey").value("errors.passwordChangeRequired"))
                .andExpect(jsonPath("$.message").doesNotExist());

        // allowed: whitelisted, and reflects the flag so the frontend can redirect on refresh
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
    }

    @Test
    void preflightRequest_isNeverBlockedByTheFlag() throws Exception {
        User employee = persistEmployee("newhire2@test.com", true);
        String token = jwtUtil.generateToken(employee);

        mockMvc.perform(options("/api/users/" + employee.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void accountWithoutTheFlag_isNotAffected() throws Exception {
        User employee = persistEmployee("regular@test.com", false);
        String token = jwtUtil.generateToken(employee);

        mockMvc.perform(get("/api/users/" + employee.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void changingThePassword_clearsTheFlag_issuesAWorkingToken_andKillsTheOldOne() throws Exception {
        User employee = persistEmployee("newhire3@test.com", true);
        String oldToken = jwtUtil.generateToken(employee);

        String body = "{\"currentPassword\":\"Temp123!\",\"newPassword\":\"NewPass1!\",\"confirmPassword\":\"NewPass1!\"}";

        String responseJson = mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(responseJson);
        String newToken = json.get("token").asText();

        // the old token's tokenVersion is now stale
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());

        // the new token works normally — no longer blocked
        mockMvc.perform(get("/api/users/" + employee.getId()).header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    // A wrong current password (BadCredentialsException -> 401, lockout counter bumped, flag
    // untouched) is covered at the unit level in AuthServiceTest instead of here: it goes through
    // LoginAttemptService's REQUIRES_NEW transaction, which a @Transactional integration test's
    // own ambient transaction interacts badly with (a well-known Spring-test/REQUIRES_NEW
    // combination, unrelated to this feature) — the unit test exercises the real behavior without
    // that collision.
}
