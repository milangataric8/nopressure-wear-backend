package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.nopressurewear.dto.email.FailedEmailResponse;
import rs.nopressurewear.model.FailedEmail;
import rs.nopressurewear.repository.FailedEmailRepository;

import java.util.List;
import java.util.Map;

/**
 * Read-only admin visibility into the email retry queue. An abandoned email means a
 * customer paid or registered and never got the mail — worth resending manually or
 * following up.
 */
@RestController
@RequestMapping("/api/failed-emails")
@RequiredArgsConstructor
public class FailedEmailController {

    private final FailedEmailRepository repository;

    @GetMapping("/abandoned")
    public ResponseEntity<List<FailedEmailResponse>> abandoned() {
        return ResponseEntity.ok(
                repository.findByStatusOrderByCreatedAtDesc(FailedEmail.ABANDONED).stream()
                        .map(FailedEmailResponse::from)
                        .toList());
    }

    @GetMapping("/abandoned/count")
    public ResponseEntity<Map<String, Long>> abandonedCount() {
        return ResponseEntity.ok(Map.of("count", repository.countByStatus(FailedEmail.ABANDONED)));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FailedEmailResponse>> pending() {
        return ResponseEntity.ok(
                repository.findByStatusOrderByCreatedAtDesc(FailedEmail.PENDING).stream()
                        .map(FailedEmailResponse::from)
                        .toList());
    }
}
