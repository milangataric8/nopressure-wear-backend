package rs.nopressurewear.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * URL-level access control matrix (spec section 11). SUPER_ADMIN > ADMIN > EMPLOYEE:
 * catalog is EMPLOYEE+, general management is ADMIN+, Reports/Dashboard/Employees are
 * SUPER_ADMIN only, and nothing admin is reachable by a customer or anonymously.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@Transactional
class RoleBasedAccessControlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private tools.jackson.databind.ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    // ---------- EMPLOYEE ----------

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotReadReports() throws Exception {
        mockMvc.perform(get("/api/reports/orders/pdf")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotReadDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotListEmployees() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotListAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders/all")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canListProducts() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                        .content("{\"name\":\"RBAC Tee\",\"price\":9.99,\"sku\":\"RBAC-EMP\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    // ---------- ADMIN ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_cannotReadReports() throws Exception {
        mockMvc.perform(get("/api/reports/orders/pdf")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_cannotListEmployees() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canListAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders/all")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateProduct_viaHierarchy() throws Exception {
        mockMvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                        .content("{\"name\":\"RBAC Tee A\",\"price\":9.99,\"sku\":\"RBAC-ADM\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_cannotUpdateEmployeeOrItsRole() throws Exception {
        mockMvc.perform(put("/api/employees/1").contentType(APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"admin403@test.com\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- SUPER_ADMIN ----------

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdmin_canReadReports() throws Exception {
        mockMvc.perform(get("/api/reports/orders/pdf")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdmin_canReadDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdmin_canListEmployees() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdmin_canListAllOrders_viaHierarchy() throws Exception {
        mockMvc.perform(get("/api/orders/all")).andExpect(status().isOk());
    }

    // These two need a *real* User principal (not @WithMockUser's stub UserDetails) because
    // EmployeeService.update()'s self-change guard reads AuthUtil.getCurrentUser(), which casts
    // the principal to rs.nopressurewear.model.User.

    @Test
    void superAdmin_canGrantSuperAdminToExistingAccount_viaUpdate() throws Exception {
        String auth = bearerForNewSuperAdmin("actor1@test.com");
        long id = createEmployee("grant@test.com", auth);

        mockMvc.perform(put("/api/employees/" + id).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"firstName\":\"Grant\",\"lastName\":\"Ee\",\"email\":\"grant@test.com\",\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }

    @Test
    void superAdmin_rejectsUnassignableRoleOnUpdate() throws Exception {
        String auth = bearerForNewSuperAdmin("actor2@test.com");
        long id = createEmployee("badrole@test.com", auth);

        mockMvc.perform(put("/api/employees/" + id).header("Authorization", auth).contentType(APPLICATION_JSON)
                        // CUSTOMER is a real Role value but not one this endpoint may assign
                        .content("{\"firstName\":\"Bad\",\"lastName\":\"Role\",\"email\":\"badrole@test.com\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isBadRequest());
    }

    private String bearerForNewSuperAdmin(String email) {
        User superAdmin = userRepository.save(User.builder()
                .firstName("Actor").lastName("Super").email(email).password("x")
                .role(Role.SUPER_ADMIN).isActive(true).build());
        return "Bearer " + jwtUtil.generateToken(superAdmin);
    }

    private long createEmployee(String email, String authHeader) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "firstName", "First", "lastName", "Last", "email", email,
                "password", "Passw0rd!", "role", "EMPLOYEE"));
        String response = mockMvc.perform(post("/api/employees").header("Authorization", authHeader)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // ---------- customer / anonymous ----------

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_cannotReachAnyAdminArea() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/all")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/orders/pdf")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isForbidden());
    }

    @Test
    void anonymous_getsUnauthorizedOnAdminArea() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/all")).andExpect(status().isUnauthorized());
    }
}
