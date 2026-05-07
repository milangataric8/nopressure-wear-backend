package rs.nopressure.wear.webshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String token) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText(
                "You requested a password reset.\n\n" +
                        "Click the link below to reset your password:\n" +
                        "http://localhost:5173/reset-password?token=" + token + "\n\n" +
                        "This link expires in 1 hour.\n\n" +
                        "If you did not request this, please ignore this email."
        );
        mailSender.send(message);
    }

    public void sendOrderStatusEmail(String to, Long orderId, String status) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@webshop.com");
        message.setTo(to);
        message.setSubject("Order #" + orderId + " Status Update");
        message.setText(
                "Hello,\n\n" +
                        "Your order #" + orderId + " status has been updated to: " + status + "\n\n" +
                        "You can track your order at: http://localhost:5173/orders/" + orderId + "\n\n" +
                        "Thank you for shopping with us!\n\n" +
                        "WebShop Team"
        );
        mailSender.send(message);
    }
}