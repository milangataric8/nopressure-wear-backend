package rs.nopressurewear.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.nopressurewear.dto.auth.AuthResponse;
import rs.nopressurewear.dto.auth.LoginRequest;
import rs.nopressurewear.dto.auth.RegisterRequest;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.EmailVerificationTokenRepository;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.repository.UserRepository;
import rs.nopressurewear.security.JwtUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static rs.nopressurewear.constants.Role.CUSTOMER;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @Mock
    private StoreSettingsRepository storeSettingsRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("Milan")
                .lastName("Gataric")
                .email("milan@nopressurewear.com")
                .password("encodedPassword")
                .role(CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Milan");
        registerRequest.setLastName("Gataric");
        registerRequest.setEmail("milan@nopressurewear.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("milan@nopressurewear.com");
        loginRequest.setPassword("password123");

        lenient().when(storeSettingsRepository.findByKey(anyString())).thenReturn(Optional.empty());
        lenient().when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().doNothing().when(tokenRepository).invalidatePreviousTokens(any());
        lenient().doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void register_ShouldReturnAuthResponse_WhenValidRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest, "en");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("milan@nopressurewear.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowDuplicateException_WhenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest, "en"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenValidCredentials() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void login_ShouldThrowException_WhenInvalidCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}