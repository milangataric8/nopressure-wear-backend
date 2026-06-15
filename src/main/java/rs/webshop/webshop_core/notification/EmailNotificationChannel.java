package rs.webshop.webshop_core.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.webshop.webshop_core.service.EmailService;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailService emailService;

    @Override
    public String getName() {
        return "EMAIL";
    }

    @Override
    public boolean isEnabled() {
        return true; // email always available
    }

    @Override
    public void send(String recipient, String subject, String message, String imageUrl) {
        emailService.sendNotificationEmail(recipient, subject, message, imageUrl);
    }
}