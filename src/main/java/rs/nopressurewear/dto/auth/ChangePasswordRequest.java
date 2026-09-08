package rs.nopressurewear.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    // No @Size/@Pattern here on purpose: this is the caller proving they know the *existing*
    // password, which may predate today's strength rules (or be a temp password an admin typed
    // in) — it only ever needs to match what's stored, checked in AuthService.
    @NotBlank(message = "validation.currentPassword.required")
    private String currentPassword;

    @NotBlank(message = "validation.newPassword.required")
    @Size(min = 8, message = "validation.newPassword.minLength")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
            message = "validation.newPassword.format"
    )
    private String newPassword;

    @NotBlank(message = "validation.confirmPassword.required")
    private String confirmPassword;
}
