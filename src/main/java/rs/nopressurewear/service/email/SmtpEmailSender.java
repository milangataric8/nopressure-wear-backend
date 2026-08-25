package rs.nopressurewear.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * SMTP transport for everything except production (local, ci). Points at Mailtrap's
 * sandbox in {@code application-local.yml} — mail never leaves the test inbox.
 */
@Service
@Profile("!prod")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-email:nopressure.wear.official@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:NoPressure Wear}")
    private String fromName;

    @Override
    public void send(String to, String subject, String htmlContent, String replyTo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail, fromName);
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("SMTP send failed: " + e.getMessage(), e);
        }
    }
}
