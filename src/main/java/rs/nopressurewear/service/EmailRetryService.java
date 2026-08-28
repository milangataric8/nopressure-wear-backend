package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.model.FailedEmail;
import rs.nopressurewear.repository.FailedEmailRepository;
import rs.nopressurewear.service.email.EmailTransport;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Works off the {@code failed_email} queue: re-sends {@code PENDING} rows that are due,
 * with exponential backoff, and gives up after {@code max-attempts}. Also purges old
 * {@code SENT} rows once a day.
 *
 * <p>Sends through the raw {@link EmailTransport}, <b>not</b> the retrying decorator —
 * otherwise a failed retry would enqueue a second copy and the table would grow
 * without bound.
 */
@Service
@RequiredArgsConstructor
public class EmailRetryService {

    private static final Logger log = LoggerFactory.getLogger(EmailRetryService.class);

    private final FailedEmailRepository repository;
    private final EmailTransport transport;

    @Value("${email.retry.enabled:true}")
    private boolean enabled;

    @Value("${email.retry.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${email.retry.interval-ms:120000}",
               initialDelayString = "${email.retry.interval-ms:120000}")
    @Transactional
    public void retryFailedEmails() {
        if (!enabled) return;

        List<FailedEmail> batch = repository
                .findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                        FailedEmail.PENDING, LocalDateTime.now());
        if (batch.isEmpty()) return;

        int sent = 0;
        for (FailedEmail email : batch) {
            try {
                transport.send(email.getRecipient(), email.getSubject(), email.getHtmlContent());
                email.setStatus(FailedEmail.SENT);
                email.setSentAt(LocalDateTime.now());
                sent++;
                log.info("Retry succeeded for {} after {} attempt(s)",
                        email.getRecipient(), email.getAttempts());
            } catch (Exception e) {
                int attempts = email.getAttempts() + 1;
                email.setAttempts(attempts);
                email.setLastError(truncate(e.getMessage(), 1000));

                if (attempts >= maxAttempts) {
                    email.setStatus(FailedEmail.ABANDONED);
                    log.error("Giving up on email to {} (subject: {}) after {} attempts: {}",
                            email.getRecipient(), email.getSubject(), attempts, email.getLastError());
                } else {
                    // exponential backoff: 2, 4, 8, 16 minutes
                    email.setNextRetryAt(LocalDateTime.now()
                            .plusMinutes((long) Math.pow(2, attempts)));
                }
            }
            repository.save(email);
        }
        log.info("Email retry run: {} sent, {} still pending/abandoned", sent, batch.size() - sent);
    }

    /** 3:30 AM daily — {@code SENT} rows are kept a week for troubleshooting, then removed. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOldSent() {
        repository.deleteSentBefore(LocalDateTime.now().minusDays(7));
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
