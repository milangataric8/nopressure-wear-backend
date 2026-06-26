package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.dto.auth.*;
import rs.nopressurewear.exception.*;
import rs.nopressurewear.model.EmailVerificationToken;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.EmailVerificationTokenRepository;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.repository.UserRepository;
import rs.nopressurewear.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.UUID;

import static rs.nopressurewear.constants.Role.CUSTOMER;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final StoreSettingsRepository storeSettingsRepository;
    private final EmailVerificationTokenRepository tokenRepository;

    private static final long VERIFICATION_TTL_HOURS = 24;

    private boolean isSettingEnabled(String key) {
        return storeSettingsRepository.findByKey(key)
                .map(s -> !"false".equalsIgnoreCase(s.getValue()))
                .orElse(true);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String lang) {
        if (!isSettingEnabled("registration_enabled")) {
            throw new RegistrationDisabledException("Registration is currently disabled");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with this email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(CUSTOMER)
                .isActive(true)
                .emailVerified(false)
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
        emailService.sendVerificationEmail(user.getEmail(), token, lang);
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
        if (user != null && !user.isEmailVerified()) {
            sendVerificationEmail(user, lang);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                .build();
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

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}