package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.contact.ContactRequest;
import rs.webshop.webshop_core.service.EmailService;

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
                request.getMessage(),
                lang
        );

        emailService.sendContactConfirmation(
                request.getEmail(),
                request.getName(),
                lang
        );

        return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
    }
}