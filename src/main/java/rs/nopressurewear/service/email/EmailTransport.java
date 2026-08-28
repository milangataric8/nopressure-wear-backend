package rs.nopressurewear.service.email;

/**
 * Low-level transport that actually hands an already-built HTML email to a delivery
 * provider. One implementation is active per environment, selected by Spring profile:
 * <ul>
 *     <li>{@link SmtpEmailSender} ({@code !prod}) — SMTP via Mailtrap, for local/dev.</li>
 *     <li>{@link BrevoEmailSender} ({@code prod}) — Brevo HTTP API, since Railway blocks
 *     outbound SMTP ports.</li>
 * </ul>
 *
 * <p>A transport is expected to throw when delivery fails — callers decide what to do
 * with that. {@link EmailSender} (the {@code @Primary} bean everything else injects) is
 * the failure-capturing decorator around this; {@code EmailRetryService} deliberately
 * injects the raw {@code EmailTransport} instead, so a failed retry doesn't enqueue a
 * second copy of the same email.
 *
 * <p>All HTML template, translation, and content logic lives in {@code EmailService};
 * this interface only cares about actually delivering the built HTML.
 */
public interface EmailTransport {

    /**
     * @param to          recipient address
     * @param subject     email subject
     * @param htmlContent fully-built HTML body (image references must be absolute URLs —
     *                    {@code cid:} inline attachments are not supported by the Brevo API)
     * @param replyTo     optional Reply-To address, or {@code null}/blank to omit it
     */
    void send(String to, String subject, String htmlContent, String replyTo);

    default void send(String to, String subject, String htmlContent) {
        send(to, subject, htmlContent, null);
    }
}
