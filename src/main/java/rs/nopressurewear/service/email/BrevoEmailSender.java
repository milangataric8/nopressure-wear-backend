package rs.nopressurewear.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production transport: Brevo's transactional email HTTP API. Railway blocks outbound
 * SMTP (587/2525 both time out), but HTTP on 443 is never blocked, so this replaces
 * SMTP for the {@code prod} profile.
 *
 * <p>Note: the Brevo API has no support for {@code cid:} inline image attachments —
 * every image referenced in the HTML must be an absolute URL. {@code EmailService}
 * builds HTML accordingly.
 */
@Slf4j
@Service
@Profile("prod")
public class BrevoEmailSender implements EmailSender {

    private final WebClient webClient;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailSender(@Value("${app.brevo.api-key}") String apiKey,
                             @Value("${app.brevo.sender-email}") String senderEmail,
                             @Value("${app.brevo.sender-name:NoPressure Wear}") String senderName) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", "application/json")
                .build();
    }

    @Override
    public void send(String to, String subject, String htmlContent, String replyTo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of("email", senderEmail, "name", senderName));
        payload.put("to", List.of(Map.of("email", to)));
        payload.put("subject", subject);
        payload.put("htmlContent", htmlContent);
        if (replyTo != null && !replyTo.isBlank()) {
            payload.put("replyTo", Map.of("email", replyTo));
        }

        try {
            webClient.post()
                    .uri("/v3/smtp/email")
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(15))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Brevo send failed [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Brevo API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new RuntimeException("Brevo send failed: " + e.getMessage(), e);
        }
    }
}
