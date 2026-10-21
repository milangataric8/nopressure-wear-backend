package rs.nopressurewear.notifications.api.dto.email;

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
) {}
