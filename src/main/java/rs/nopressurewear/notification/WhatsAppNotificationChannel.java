package rs.nopressurewear.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WhatsAppNotificationChannel implements NotificationChannel {

    @Value("${app.twilio.enabled:false}")
    private boolean enabled;

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    @Value("${app.twilio.whatsapp-from:}")
    private String whatsappFrom;

    @Override
    public String getName() {
        return "WHATSAPP";
    }

    @Override
    public boolean isEnabled() {
        return enabled && !accountSid.isBlank();
    }

    @Override
    public void send(String recipient, String subject, String message, String imageUrl, String bgColor, String textColor) {
        if (!isEnabled()) {
            log.warn("WhatsApp channel not configured — skipping message to {}", recipient);
            return;
        }
        // TODO: Twilio supports media via .setMediaUrl(List.of(imageUrl)) on Message.creator
        log.info("[WhatsApp STUB] Would send to {} with image {}: {}", recipient, imageUrl, message);
    }
}