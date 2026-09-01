package rs.nopressurewear.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.dto.user.UserUpdateRequest;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;
import rs.nopressurewear.service.EmployeeService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end version of the "after a role change, the old token stops working" rule
 * (backend-employee-role-edit.md section 5 / 7 test table).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@Transactional
class TokenInvalidationOnRoleChangeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeService employeeService;

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

    @Test
    void tokenIssuedBeforeARoleChange_isRejectedAfterwards() throws Exception {
        User superAdmin = userRepository.save(User.builder()
                .firstName("Owner").lastName("Account").email("owner@test.com")
                .password("x").role(Role.SUPER_ADMIN).isActive(true).build());
        User target = userRepository.save(User.builder()
                .firstName("Target").lastName("Employee").email("target@test.com")
                .password("x").role(Role.EMPLOYEE).isActive(true).build());

        String tokenIssuedBeforeChange = jwtUtil.generateToken(target);

        // sanity check: the token authenticates before the role change (403 = authenticated as
        // EMPLOYEE but not authorized for this SUPER_ADMIN-only endpoint — proves the token works)
        mockMvc.perform(get("/api/dashboard/overview").header("Authorization", "Bearer " + tokenIssuedBeforeChange))
                .andExpect(status().isForbidden());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(superAdmin, null, superAdmin.getAuthorities()));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Target");
        request.setLastName("Employee");
        request.setEmail("target@test.com");
        request.setRole(Role.ADMIN);
        employeeService.update(target.getId(), request);

        mockMvc.perform(get("/api/dashboard/overview").header("Authorization", "Bearer " + tokenIssuedBeforeChange))
                .andExpect(status().isUnauthorized());
    }
}
