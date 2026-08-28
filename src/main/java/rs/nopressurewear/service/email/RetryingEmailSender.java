package rs.nopressurewear.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import rs.nopressurewear.model.FailedEmail;
import rs.nopressurewear.repository.FailedEmailRepository;

import java.time.LocalDateTime;

/**
 * Wraps the active {@link EmailTransport} so a failed send is persisted to the
 * {@code failed_email} queue instead of being lost. The exception is <b>never</b>
 * rethrown: registration, checkout and the like must not fail because an email
 * couldn't go out — it's queued and {@code EmailRetryService} will deliver it later.
 *
 * <p>This is the {@code @Primary} {@link EmailSender}; the concrete provider clients
 * implement {@link EmailTransport}, so there's no self-injection.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class RetryingEmailSender implements EmailSender {

    /** Brevo (prod) or SMTP (non-prod) — exactly one is active per profile. */
    private final EmailTransport delegate;
    private final FailedEmailRepository failedEmailRepository;

    @Override
    public void send(String to, String subject, String htmlContent, String replyTo) {
        try {
            delegate.send(to, subject, htmlContent, replyTo);
        } catch (Exception e) {
            // Deliberately not rethrown — a delivery problem must not turn into a lost
            // customer. The message is queued and retried on a schedule.
            // Note: replyTo is not persisted (rare, admin-only contact mail); a retried
            // send simply goes out without a Reply-To header.
            log.error("Email send failed for {} — queued for retry: {}", to, e.getMessage());
            failedEmailRepository.save(FailedEmail.builder()
                    .recipient(to)
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .attempts(1)
                    .lastError(truncate(e.getMessage(), 1000))
                    .status(FailedEmail.PENDING)
                    .nextRetryAt(LocalDateTime.now().plusMinutes(2))
                    .build());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
