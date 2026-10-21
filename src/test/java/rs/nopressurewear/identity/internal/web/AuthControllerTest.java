package rs.nopressurewear.identity.internal.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import rs.nopressurewear.identity.api.dto.auth.AuthResponse;
import rs.nopressurewear.identity.api.dto.auth.ChangePasswordRequest;
import rs.nopressurewear.identity.api.dto.auth.LoginRequest;
import rs.nopressurewear.identity.api.dto.auth.RegisterRequest;
import rs.nopressurewear.storefront.api.SettingsQuery;
import rs.nopressurewear.identity.internal.security.AuthUtil;
import rs.nopressurewear.identity.internal.security.JwtUtil;
import rs.nopressurewear.identity.internal.security.UserDetailsServiceImpl;
import rs.nopressurewear.identity.internal.application.AuthService;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private SettingsQuery storeSettingsRepository;

    @MockitoBean
    private AuthUtil authUtil;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @Test
    @WithMockUser
    void register_ShouldReturn201_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Milan");
        request.setLastName("Gataric");
        request.setEmail("milan@nopressurewear.com");
        request.setPassword("Password1!");

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .token("jwt-token")
                .email("milan@nopressurewear.com")
                .role("CUSTOMER")
                .firstName("Milan")
                .lastName("Gataric")
                .build();

        when(authService.register(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("milan@nopressurewear.com"));
    }

    @Test
    @WithMockUser
    void register_ShouldReturn400_WhenInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /** backend-message-i18n-spec.md 3.2/3.3: DTO annotations carry i18n keys, and
     * MethodArgumentNotValidException surfaces them as fieldErrors — never literal text. */
    @Test
    @WithMockUser
    void register_ShouldReturnDottedI18nKeysInFieldErrors_WhenInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").value("validation.firstName.required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("validation.email.required"))
                .andExpect(jsonPath("$.fieldErrors.password").value("validation.password.required"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @WithMockUser
    void login_ShouldReturn200_WhenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("milan@nopressurewear.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .token("jwt-token")
                .email("milan@nopressurewear.com")
                .role("CUSTOMER")
                .firstName("Milan")
                .lastName("Gataric")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @WithMockUser
    void login_ShouldReturn400_WhenInvalidRequest() throws Exception {
        LoginRequest request = new LoginRequest();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void changePassword_ShouldOperateOnTheCallersOwnAccount_NotAPathParameter() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass1!");
        request.setNewPassword("NewPass1!");
        request.setConfirmPassword("NewPass1!");

        AuthResponse response = AuthResponse.builder()
                .id(1L).token("new-jwt-token").email("milan@nopressurewear.com")
                .role("EMPLOYEE").passwordChangeRequired(false).build();

        when(authUtil.getCurrentUserId()).thenReturn(1L);
        when(authService.changePassword(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(false));
    }

    @Test
    @WithMockUser
    void me_ShouldReturnTheCallersOwnInfo_IncludingPasswordChangeRequired() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L).email("milan@nopressurewear.com").role("EMPLOYEE")
                .passwordChangeRequired(true).build();

        when(authUtil.getCurrentUserId()).thenReturn(1L);
        when(authService.me(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
    }
}