package rs.webshop.webshop_core.dto.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rs.webshop.webshop_core.constants.Role;

@Getter
@Setter
public class UserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
            message = "Password must contain at least one number and one special character"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;
}