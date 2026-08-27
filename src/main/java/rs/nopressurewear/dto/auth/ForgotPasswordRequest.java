package rs.nopressurewear.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "validation.emailRequired")
    @Email(message = "validation.emailInvalid")
    private String email;
}