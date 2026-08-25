package rs.nopressurewear.service.email;

/**
 * Transport abstraction for outgoing email. One implementation per environment,
 * selected by Spring profile:
 * <ul>
 *     <li>{@link SmtpEmailSender} ({@code !prod}) — SMTP via Mailtrap, for local/dev.</li>
 *     <li>{@link BrevoEmailSender} ({@code prod}) — Brevo HTTP API, since Railway blocks
 *     outbound SMTP ports.</li>
 * </ul>
 * All HTML template, translation, and content logic lives in {@code EmailService};
 * this interface only cares about actually delivering an already-built HTML email.
 */
public interface EmailSender {

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
