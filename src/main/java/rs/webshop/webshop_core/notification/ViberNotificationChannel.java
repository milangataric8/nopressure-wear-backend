package rs.webshop.webshop_core.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ViberNotificationChannel implements NotificationChannel {

    @Value("${app.viber.enabled:false}")
    private boolean enabled;

    @Value("${app.viber.auth-token:}")
    private String authToken;

    @Override
    public String getName() {
        return "VIBER";
    }

    @Override
    public boolean isEnabled() {
        return enabled && !authToken.isBlank();
    }

    @Override
    public void send(String recipient, String subject, String message, String imageUrl) {
        if (!isEnabled()) {
            log.warn("Viber channel not configured — skipping message to {}", recipient);
            return;
        }
        log.info("[Viber STUB] Would send to {} with image {}: {}", recipient, imageUrl, message);
    }
}