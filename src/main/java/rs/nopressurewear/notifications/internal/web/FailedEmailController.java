package rs.nopressurewear.notifications.internal.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.nopressurewear.notifications.api.dto.email.FailedEmailResponse;
import rs.nopressurewear.notifications.internal.domain.FailedEmail;
import rs.nopressurewear.notifications.internal.persistence.FailedEmailRepository;

import java.util.List;
import java.util.Map;

import static rs.nopressurewear.notifications.internal.domain.FailedEmail.ABANDONED;
import static rs.nopressurewear.notifications.internal.domain.FailedEmail.PENDING;

/**
 * Read-only admin visibility into the email retry queue. An abandoned email means a
 * customer paid or registered and never got the mail — worth resending manually or
 * following up.
 */
@RestController
@RequestMapping("/api/failed-emails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FailedEmailController {

    private final FailedEmailRepository repository;

    @GetMapping("/abandoned")
    public ResponseEntity<List<FailedEmailResponse>> abandoned() {
        return ResponseEntity.ok(
                repository.findByStatusOrderByCreatedAtDesc(ABANDONED).stream()
                        .map(FailedEmailController::toResponse)
                        .toList());
    }

    @GetMapping("/abandoned/count")
    public ResponseEntity<Map<String, Long>> abandonedCount() {
        return ResponseEntity.ok(Map.of("count", repository.countByStatus(ABANDONED)));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FailedEmailResponse>> pending() {
        return ResponseEntity.ok(
                repository.findByStatusOrderByCreatedAtDesc(PENDING).stream()
                        .map(FailedEmailController::toResponse)
                        .toList());
    }

    private static FailedEmailResponse toResponse(FailedEmail email) {
        return new FailedEmailResponse(email.getId(), email.getRecipient(), email.getSubject(),
                email.getStatus(), email.getAttempts(), email.getLastError(), email.getCreatedAt(),
                email.getNextRetryAt(), email.getSentAt());
    }
}
