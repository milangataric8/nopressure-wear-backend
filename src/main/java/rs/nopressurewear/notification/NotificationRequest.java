package rs.nopressurewear.dto.notification;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NotificationRequest {
    private String subject;

    @NotBlank(message = "Message is required")
    private String message;

    private String imageUrl;

    @NotEmpty(message = "At least one channel required")
    private List<String> channels;
}