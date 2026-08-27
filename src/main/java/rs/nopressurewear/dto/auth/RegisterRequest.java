package rs.nopressurewear.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "validation.firstNameRequired")
    private String firstName;

    @NotBlank(message = "validation.lastNameRequired")
    private String lastName;

    @NotBlank(message = "validation.emailRequired")
    @Email(message = "validation.emailInvalid")
    private String email;

    @NotBlank(message = "validation.passwordRequired")
    @Size(min = 8, message = "validation.passwordTooShort")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
            message = "validation.passwordWeak"
    )
    private String password;
}