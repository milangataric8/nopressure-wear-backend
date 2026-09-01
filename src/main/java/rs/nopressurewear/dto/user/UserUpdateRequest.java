package rs.nopressurewear.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.Role;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
            message = "Password must contain at least one number and one special character"
    )
    private String password;

    /**
     * Present only on the employee-management update ({@code EmployeeService.update}); a
     * customer's own profile update ({@code UserService.updateProfile}) ignores this field
     * entirely, so a value here can never escalate a customer's own role. {@code null} means
     * "leave the role as it is".
     */
    private Role role;
}