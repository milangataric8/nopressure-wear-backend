package rs.nopressurewear.service.email;

/**
 * The entry point every caller ({@code EmailService} and below) injects for sending
 * mail. The one bean implementing it is {@link RetryingEmailSender}, a {@code @Primary}
 * decorator that delegates to the active {@link EmailTransport} and, on failure,
 * persists the message to the {@code failed_email} queue instead of letting the
 * exception propagate — so a delivery problem never breaks registration or checkout.
 *
 * <p>The concrete provider clients ({@link BrevoEmailSender}, {@link SmtpEmailSender})
 * implement {@link EmailTransport}, not this interface.
 */
public interface EmailSender {

    /**
     * @param to          recipient address
     * @param subject     email subject
     * @param htmlContent fully-built HTML body (image references must be absolute URLs)
     * @param replyTo     optional Reply-To address, or {@code null}/blank to omit it
     */
    void send(String to, String subject, String htmlContent, String replyTo);

    default void send(String to, String subject, String htmlContent) {
        send(to, subject, htmlContent, null);
    }
}
