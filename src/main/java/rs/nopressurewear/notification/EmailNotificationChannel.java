package rs.nopressurewear.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.nopressurewear.service.EmailService;

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