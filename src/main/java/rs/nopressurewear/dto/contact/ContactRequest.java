package rs.nopressurewear.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {
    @NotBlank(message = "validation.nameRequired")
    private String name;

    @NotBlank(message = "validation.emailRequired")
    @Email(message = "validation.emailInvalid")
    private String email;

    private String subject;

    @NotBlank(message = "validation.messageRequired")
    private String message;
}