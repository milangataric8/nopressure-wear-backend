package rs.nopressurewear.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.nopressurewear.dto.auth.AuthResponse;
import rs.nopressurewear.dto.auth.ChangePasswordRequest;
import rs.nopressurewear.dto.auth.LoginRequest;
import rs.nopressurewear.dto.auth.RegisterRequest;
import rs.nopressurewear.exception.AccountLockedException;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.EmailVerificationTokenRepository;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.repository.UserRepository;
import rs.nopressurewear.security.JwtUtil;

import java.time.LocalDateTime;
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
        lenient().doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), any());
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
    void register_ShouldPersistTheUisLanguageAsPreferredLocale() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        authService.register(registerRequest, "en");

        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPreferredLocale()).isEqualTo("en");
    }

    /** backend-message-i18n-spec.md section 4.3, acceptance criterion: the recipient's own
     * preferred_locale wins for a verification (re)send, regardless of the request's own lang. */
    @Test
    void sendVerificationEmail_UsesTheUsersOwnPreferredLocale_NotTheRequestLang() {
        user.setPreferredLocale("en");
        ArgumentCaptor<java.util.Locale> localeCaptor = ArgumentCaptor.forClass(java.util.Locale.class);

        authService.sendVerificationEmail(user, "sr");

        verify(emailService).sendVerificationEmail(anyString(), anyString(), localeCaptor.capture());
        assertThat(localeCaptor.getValue()).isEqualTo(java.util.Locale.forLanguageTag("en"));
    }

    @Test
    void register_ShouldThrowDuplicateException_WhenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest, "en"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("validation.email.taken");

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

    @Test
    void login_ShouldReturnPasswordChangeRequiredTrue_WhenFlagIsSet() {
        user.setPasswordChangeRequired(true);
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.isPasswordChangeRequired()).isTrue();
    }

    // ---------- changePassword (backend-forced-password-change.md section 5 / 7) ----------

    private ChangePasswordRequest changePasswordRequest(String current, String next, String confirm) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(next);
        request.setConfirmPassword(confirm);
        return request;
    }

    @Test
    void changePassword_ShouldClearFlagAndReturnNewToken_WhenCurrentPasswordCorrect() {
        user.setPasswordChangeRequired(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any())).thenReturn("new-jwt-token");

        AuthResponse response = authService.changePassword(1L,
                changePasswordRequest("OldPass1!", "NewPass1!", "NewPass1!"));

        assertThat(response.getToken()).isEqualTo("new-jwt-token");
        assertThat(response.isPasswordChangeRequired()).isFalse();
        assertThat(user.isPasswordChangeRequired()).isFalse();
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
    }

    @Test
    void changePassword_ShouldThrowBadCredentials_WhenCurrentPasswordWrong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L,
                changePasswordRequest("WrongPass1!", "NewPass1!", "NewPass1!")))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService, times(1)).registerFailedAttempt(user);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_ShouldThrowFieldValidation_WhenNewAndConfirmDontMatch() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "encodedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(1L,
                changePasswordRequest("OldPass1!", "NewPass1!", "Different1!")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("validation.confirmPassword.mismatch");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_ShouldThrowFieldValidation_WhenNewPasswordSameAsCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePass1!", "encodedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(1L,
                changePasswordRequest("SamePass1!", "SamePass1!", "SamePass1!")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("validation.newPassword.sameAsCurrent");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_ShouldThrowAccountLocked_WhenAccountIsLocked() {
        user.setLockUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.changePassword(1L,
                changePasswordRequest("OldPass1!", "NewPass1!", "NewPass1!")))
                .isInstanceOf(AccountLockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }
}