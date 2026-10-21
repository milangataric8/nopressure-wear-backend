package rs.nopressurewear.notifications.internal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.notifications.api.dto.contact.ContactRequest;
import rs.nopressurewear.notifications.internal.application.EmailService;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Map<String, String>> sendContact(
            @Valid @RequestBody ContactRequest request,
            @RequestParam(defaultValue = "en") String lang) {

        emailService.sendContactEmail(
                request.getName(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage()
        );

        emailService.sendContactConfirmation(
                request.getEmail(),
                request.getName(),
                lang
        );

        return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
    }
}