package rs.nopressurewear.identity.internal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.identity.api.dto.auth.AuthResponse;
import rs.nopressurewear.identity.api.dto.auth.ChangePasswordRequest;
import rs.nopressurewear.identity.api.dto.auth.ForgotPasswordRequest;
import rs.nopressurewear.identity.api.dto.auth.LoginRequest;
import rs.nopressurewear.identity.api.dto.auth.RegisterRequest;
import rs.nopressurewear.identity.api.dto.auth.ResendRequest;
import rs.nopressurewear.identity.api.dto.auth.ResetPasswordRequest;
import rs.nopressurewear.identity.internal.security.AuthUtil;
import rs.nopressurewear.identity.internal.application.AuthService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthUtil authUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity
                .status(CREATED)
                .body(authService.register(request, lang));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendRequest req,
                                                   @RequestParam(defaultValue = "en") String lang) {
        authService.resendVerification(req.getEmail(), lang);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                               @RequestParam(defaultValue = "en") String lang) {
        authService.forgotPassword(request, lang);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /** Always the caller's own account — {@code userId} comes from the token, never the request. */
    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authUtil.getCurrentUserId(), request));
    }

    /** Polled by the frontend on page refresh to know whether to redirect into the forced
     * password-change screen — the login response alone doesn't survive a reload. */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        return ResponseEntity.ok(authService.me(authUtil.getCurrentUserId()));
    }
}