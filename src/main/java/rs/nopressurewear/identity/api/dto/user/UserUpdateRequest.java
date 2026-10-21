package rs.nopressurewear.identity.api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.identity.api.Role;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "validation.firstName.required")
    private String firstName;

    @NotBlank(message = "validation.lastName.required")
    private String lastName;

    @NotBlank(message = "validation.email.required")
    @Email(message = "validation.email.invalid")
    private String email;

    @Size(min = 8, message = "validation.password.minLength")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
            message = "validation.password.format"
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
