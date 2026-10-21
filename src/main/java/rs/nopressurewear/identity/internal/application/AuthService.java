package rs.nopressurewear.identity.internal.application;
import rs.nopressurewear.notifications.api.EmailOperations;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.identity.api.Role;
import rs.nopressurewear.identity.api.dto.auth.AuthResponse;
import rs.nopressurewear.identity.api.dto.auth.ChangePasswordRequest;
import rs.nopressurewear.identity.api.dto.auth.ForgotPasswordRequest;
import rs.nopressurewear.identity.api.dto.auth.LoginRequest;
import rs.nopressurewear.identity.api.dto.auth.RegisterRequest;
import rs.nopressurewear.identity.api.dto.auth.ResetPasswordRequest;
import rs.nopressurewear.platform.error.AccountLockedException;
import rs.nopressurewear.platform.error.DuplicateResourceException;
import rs.nopressurewear.platform.error.EmailNotVerifiedException;
import rs.nopressurewear.platform.error.FieldValidationException;
import rs.nopressurewear.platform.error.GlobalExceptionHandler;
import rs.nopressurewear.platform.error.InvalidTokenException;
import rs.nopressurewear.platform.error.LoginDisabledException;
import rs.nopressurewear.platform.error.RegistrationDisabledException;
import rs.nopressurewear.platform.error.ResourceNotFoundException;
import rs.nopressurewear.identity.internal.domain.EmailVerificationToken;
import rs.nopressurewear.identity.internal.domain.User;
import rs.nopressurewear.identity.internal.persistence.EmailVerificationTokenRepository;
import rs.nopressurewear.storefront.api.SettingsQuery;
import rs.nopressurewear.identity.internal.persistence.UserRepository;
import rs.nopressurewear.identity.internal.security.JwtUtil;
import rs.nopressurewear.platform.util.LocaleResolver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static rs.nopressurewear.identity.api.Role.CUSTOMER;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final SettingsQuery storeSettingsRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailOperations emailService;
    private final LoginAttemptService loginAttemptService;

    private static final long VERIFICATION_TTL_HOURS = 24;

    @Value("${security.lockout.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.lockout.lock-minutes:15}")
    private int lockMinutes;

    private boolean isSettingEnabled(String key) {
        return storeSettingsRepository.findValue(key)
                .map(s -> !"false".equalsIgnoreCase(s))
                .orElse(true);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String lang) {
        if (!isSettingEnabled("registration_enabled")) {
            throw new RegistrationDisabledException("Registration is currently disabled");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("validation.email.taken", "email");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                // the UI language at signup time, persisted so later async emails (verification
                // resend, order updates, ...) land in it regardless of who/what triggers them.
                .preferredLocale(nonNull(lang) && !lang.isBlank() ? lang : "sr")
                .build();

        User saved = userRepository.save(user);

        try {
            sendVerificationEmail(saved, lang);
        } catch (Exception e) {
            log.error("Verification email failed for {}: {}", saved.getEmail(), e.getMessage());
        }

        String jwtToken = jwtUtil.generateToken(saved);

        return AuthResponse.builder()
                .id(saved.getId())
                .token(jwtToken)
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .emailVerified(false)
                .build();
    }

    /**
     * {@code lang} is accepted for backward compatibility with the register/resend request
     * params but no longer decides the email's language: the recipient's own persisted
     * {@code preferredLocale} does (backend-message-i18n-spec.md section 4.3) — set once at
     * registration ({@link #register}), read from here on every resend, regardless of what
     * language the resend request happened to carry.
     */
    public void sendVerificationEmail(User user, String lang) {
        tokenRepository.invalidatePreviousTokens(user);
        String token = UUID.randomUUID().toString();
        EmailVerificationToken vt = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(VERIFICATION_TTL_HOURS))
                .used(false)
                .build();
        tokenRepository.save(vt);
        emailService.sendVerificationEmail(user.getEmail(), token, LocaleResolver.of(user, lang));
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken vt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification link"));

        if (vt.isUsed()) throw new InvalidTokenException("This link has already been used");
        if (vt.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new InvalidTokenException("This verification link has expired");

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        vt.setUsed(true);
        tokenRepository.save(vt);
    }

    @Transactional
    public void resendVerification(String email, String lang) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (nonNull(user) && !user.isEmailVerified()) {
            sendVerificationEmail(user, lang);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        resetCounterIfLockExpired(user);

        if (user.isLocked()) {
            long minutesLeft = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            throw new AccountLockedException("Account locked. Try again in " + minutesLeft + " minute(s).");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.registerFailedAttempt(user);
            throw ex;
        }

        resetFailedAttempts(user);

        if (!user.isEmailVerified() && isCustomer(user.getRole())) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        if (!isSettingEnabled("login_enabled") && isCustomer(user.getRole())) {
            throw new LoginDisabledException("Login is currently disabled");
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .passwordChangeRequired(user.isPasswordChangeRequired())
                .build();
    }

    /** Backs {@code GET /api/auth/me} — the frontend relies on this, not just the login response, to
     * know whether to redirect on a page refresh. No token in the response; the caller's own is still good. */
    public AuthResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AuthResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .passwordChangeRequired(user.isPasswordChangeRequired())
                .build();
    }

    private void resetCounterIfLockExpired(User user) {
        if (nonNull(user.getLockUntil()) && user.getLockUntil().isBefore(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockUntil(null);
            userRepository.save(user);
        }
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() != 0 || nonNull(user.getLockUntil())) {
            user.setFailedLoginAttempts(0);
            user.setLockUntil(null);
            userRepository.save(user);
        }
    }

    private static boolean isCustomer(Role role) {
        return role == CUSTOMER;
    }

    public void forgotPassword(ForgotPasswordRequest request, String lang) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token, lang);
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * The self-service counterpart to a forced password change (and an ordinary voluntary
     * change): always operates on the caller's own account ({@code userId} comes from the JWT,
     * never a path parameter), so this can never be used to change someone else's password.
     * Covered by the same lockout mechanism as login, since {@code currentPassword} is otherwise
     * brute-forceable here with no limit.
     */
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        resetCounterIfLockExpired(user);

        if (user.isLocked()) {
            long minutesLeft = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            throw new AccountLockedException("Account locked. Try again in " + minutesLeft + " minute(s).");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            loginAttemptService.registerFailedAttempt(user);
            // message is dev-facing only now (GlobalExceptionHandler maps BadCredentialsException
            // to a fixed "errors.invalidCredentials" regardless of content) — kept for logs.
            throw new BadCredentialsException("Current password is incorrect");
        }

        resetFailedAttempts(user);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new FieldValidationException("validation.confirmPassword.mismatch", "confirmPassword");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new FieldValidationException("validation.newPassword.sameAsCurrent", "newPassword");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);

        log.info("PASSWORD_CHANGED user={}", saved.getEmail());

        String token = jwtUtil.generateToken(saved);

        return AuthResponse.builder()
                .id(saved.getId())
                .token(token)
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .emailVerified(saved.isEmailVerified())
                .passwordChangeRequired(false)
                .build();
    }
}