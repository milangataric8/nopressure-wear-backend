package rs.nopressure.wear.webshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.nopressure.wear.webshop.dto.auth.AuthResponse;
import rs.nopressure.wear.webshop.dto.auth.LoginRequest;
import rs.nopressure.wear.webshop.dto.auth.RegisterRequest;
import rs.nopressure.wear.webshop.security.JwtUtil;
import rs.nopressure.wear.webshop.security.UserDetailsServiceImpl;
import rs.nopressure.wear.webshop.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
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

    @Test
    @WithMockUser
    void register_ShouldReturn201_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Milan");
        request.setLastName("Gataric");
        request.setEmail("milan@webshop.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .token("jwt-token")
                .email("milan@webshop.com")
                .role("CUSTOMER")
                .firstName("Milan")
                .lastName("Gataric")
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("milan@webshop.com"));
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

    @Test
    @WithMockUser
    void login_ShouldReturn200_WhenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("milan@webshop.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .token("jwt-token")
                .email("milan@webshop.com")
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
}