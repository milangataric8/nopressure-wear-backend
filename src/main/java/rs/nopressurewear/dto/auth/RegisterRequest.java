package rs.nopressurewear.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "validation.firstName.required")
    private String firstName;

    @NotBlank(message = "validation.lastName.required")
    private String lastName;

    @NotBlank(message = "validation.email.required")
    @Email(message = "validation.email.invalid")
    private String email;

    @NotBlank(message = "validation.password.required")
    @Size(min = 8, message = "validation.password.minLength")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
            message = "validation.password.format"
    )
    private String password;
}