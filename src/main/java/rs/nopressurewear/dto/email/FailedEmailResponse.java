package rs.nopressurewear.dto.email;

import rs.nopressurewear.model.FailedEmail;

import java.time.LocalDateTime;

/**
 * Admin-facing view of a queued/abandoned email. Deliberately omits {@code htmlContent}
 * (large, not useful in a list).
 */
public record FailedEmailResponse(
        Long id,
        String recipient,
        String subject,
        String status,
        int attempts,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime nextRetryAt,
        LocalDateTime sentAt
) {
    public static FailedEmailResponse from(FailedEmail e) {
        return new FailedEmailResponse(
                e.getId(),
                e.getRecipient(),
                e.getSubject(),
                e.getStatus(),
                e.getAttempts(),
                e.getLastError(),
                e.getCreatedAt(),
                e.getNextRetryAt(),
                e.getSentAt()
        );
    }
}
