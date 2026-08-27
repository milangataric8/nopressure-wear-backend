package rs.nopressurewear.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "validation.required")
    private String token;

    @NotBlank(message = "validation.passwordRequired")
    @Size(min = 8, message = "validation.passwordTooShort")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
            message = "validation.passwordWeak"
    )
    private String newPassword;
}