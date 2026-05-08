package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = "http://localhost:5173/reset-password?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                    .container { max-width: 560px; margin: 40px auto; background: #ffffff; border: 1px solid #e5e5e5; }
                    .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; }
                    .header h1 { margin: 0; font-size: 20px; font-weight: 900; text-transform: uppercase; letter-spacing: -0.5px; color: #111; }
                    .body { padding: 40px; }
                    .body p { font-size: 14px; color: #555; line-height: 1.6; margin: 0 0 24px; }
                    .button { display: inline-block; background: #111; color: #fff !important; text-decoration: none; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; padding: 14px 32px; }
                    .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; }
                    .footer p { font-size: 12px; color: #999; margin: 0; }
                    .url { font-size: 12px; color: #999; word-break: break-all; margin-top: 16px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>WebShop</h1>
                    </div>
                    <div class="body">
                        <p>You requested a password reset. Click the button below to create a new password. This link expires in <strong>1 hour</strong>.</p>
                        <a href="%s" class="button">Reset Password</a>
                        <p style="margin-top: 32px; font-size: 13px; color: #999;">If you did not request this, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 WebShop. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetUrl, resetUrl);

        sendHtmlEmail(to, "Reset your WebShop password", html);
    }

    public void sendOrderStatusEmail(String to,
                                     Long orderId,
                                     String status,
                                     String customerFirstName,
                                     String productRows,
                                     String subtotal,
                                     String shippingStreet,
                                     String shippingCity,
                                     String shippingPostalCode,
                                     String shippingCountry) {

        String orderUrl = "http://localhost:5173/orders/" + orderId;

        String statusColor = switch (status) {
            case "CONFIRMED" -> "#2563eb";
            case "SHIPPED" -> "#7c3aed";
            case "DELIVERED" -> "#16a34a";
            case "CANCELLED" -> "#dc2626";
            default -> "#d97706";
        };

        String greeting = """
            <p class="status-badge" style="color: #111; margin: 10px 0 10px;">
                Dear <strong>%s</strong>,<br><br>
                Your order with ID: <strong>#%d</strong> status has been updated to
                <span style="font-weight: 700; color: %s;">%s</span>.
            </p>
            """.formatted(customerFirstName, orderId, statusColor, status);

        String shippingSection = (shippingStreet != null && !shippingStreet.isEmpty()) ? """
            <div style="margin-top: 24px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
                <p style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; color: #111; margin: 0 0 8px;">Shipping Address</p>
                <p style="font-size: 13px; color: #555; margin: 0; line-height: 1.6;">%s<br>%s, %s<br>%s</p>
            </div>
            """.formatted(shippingStreet, shippingCity, shippingPostalCode, shippingCountry) : "";

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                    .container { max-width: 560px; margin: 40px auto; background: #ffffff; border: 1px solid #e5e5e5; }
                    .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; display: flex; justify-content: space-between; align-items: center; }
                    .header h1 { margin: 0; font-size: 20px; font-weight: 900; text-transform: uppercase; letter-spacing: -0.5px; color: #111; }
                    .body { padding: 40px; }
                    .status-badge { display: block; box-sizing: border-box; padding: 15px 20px; font-size: 11px; font-weight: 700; letter-spacing: 1px; color: %s; background: %s20; }
                    .order-title { font-size: 24px; font-weight: 900; text-transform: uppercase; color: #111; margin: 0 0 4px; }
                    .order-date { font-size: 13px; color: #999; margin: 0 0 15px; }
                    .section-title { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; color: #111; margin: 15px 0 16px; }
                    .item-row { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
                    .item-img { width: 56px; height: 56px; background: #f5f5f5; object-fit: contain; }
                    .item-name { font-size: 13px; font-weight: 600; color: #111; margin: 0 0 4px; }
                    .item-qty { font-size: 12px; color: #999; margin: 0; }
                    .item-price { font-size: 13px; font-weight: 700; color: #111; margin-left: auto; }
                    .summary-row { display: flex; justify-content: space-between; font-size: 13px; color: #555; margin-bottom: 8px; }
                    .summary-total { display: flex; justify-content: space-between; font-size: 15px; font-weight: 700; color: #111; padding-top: 12px; border-top: 1px solid #e5e5e5; margin-top: 12px; }
                    .button { display: inline-block; background: #111; color: #fff !important; text-decoration: none; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; padding: 14px 32px; margin-top: 32px; }
                    .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; }
                    .footer p { font-size: 12px; color: #999; margin: 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>WebShop</h1>
                    </div>
                    <div class="body">
                        <h2 class="order-title">Order #%d</h2>
                        <p class="order-date">Status updated</p>
                        %s
                        <p class="section-title">Items</p>
                        %s

                        <div style="margin-top: 24px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
                            <div class="summary-row"><span>Subtotal</span><span>$%s</span></div>
                            <div class="summary-row"><span>Delivery</span><span style="color: #16a34a;">Free</span></div>
                            <div class="summary-total"><span>Total</span><span>$%s</span></div>
                        </div>

                        %s

                        <a href="%s" class="button">View Order</a>
                    </div>
                    <div class="footer">
                        <p>© 2026 WebShop. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                statusColor, statusColor,
                orderId,
                greeting,
                productRows,
                subtotal, subtotal,
                shippingSection,
                orderUrl
        );

        sendHtmlEmail(to, "Your order #" + orderId + " is now " + status, html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("noreply@webshop.com");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}