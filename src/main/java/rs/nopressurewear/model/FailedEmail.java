package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * An outgoing email whose delivery failed and is queued for retry. Rows are created
 * by {@code RetryingEmailSender} when a send throws, and worked off by
 * {@code EmailRetryService} on a schedule.
 *
 * <p>{@code status}:
 * <ul>
 *     <li>{@code PENDING} — will be retried once {@code nextRetryAt} passes</li>
 *     <li>{@code SENT} — a later retry succeeded (kept a week, then purged)</li>
 *     <li>{@code ABANDONED} — gave up after {@code MAX_ATTEMPTS} (kept for inspection)</li>
 * </ul>
 */
@Entity
@Table(name = "failed_email")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FailedEmail {

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String ABANDONED = "ABANDONED";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = PENDING;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt = LocalDateTime.now();

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
