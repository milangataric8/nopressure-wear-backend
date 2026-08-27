package rs.nopressurewear.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "validation.emailRequired")
    @Email(message = "validation.emailInvalid")
    private String email;

    @NotBlank(message = "validation.passwordRequired")
    private String password;
}