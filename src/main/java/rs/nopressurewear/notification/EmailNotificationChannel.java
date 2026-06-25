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
        return true;
    }

    @Override
    public void send(String recipient, String subject, String message, String imageUrl, String bgColor, String textColor, String lang) {
        emailService.sendNotificationEmail(recipient, subject, message, imageUrl, bgColor, textColor, lang);
    }
}