package rs.nopressurewear.notifications.api.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {
    @NotBlank(message = "validation.name.required")
    private String name;

    @NotBlank(message = "validation.email.required")
    @Email(message = "validation.email.invalid")
    private String email;

    private String subject;

    @NotBlank(message = "validation.message.required")
    private String message;
}