package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;
import rs.nopressurewear.model.StoreSettings;
import rs.nopressurewear.repository.StoreSettingsRepository;

import java.io.File;
import java.math.BigDecimal;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final StoreSettingsRepository storeSettingsRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    public void sendVerificationEmail(String to, String token, String lang) {
        Map<String, String> t = getEmailTranslations(lang);
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                    .container { max-width: 560px; margin: 40px auto; background: #fff; border: 1px solid #e5e5e5; }
                    .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; text-align: center; }
                    .header h1 { margin: 0; font-size: 20px; font-weight: 900; text-transform: uppercase; color: #111; }
                    .body { padding: 40px; }
                    .body p { font-size: 14px; color: #555; line-height: 1.6; margin: 0 0 24px; }
                    .button { display: inline-block; background: #111; color: #fff !important; text-decoration: none; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; padding: 14px 32px; }
                    .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center; }
                    .footer p { font-size: 12px; color: #999; margin: 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>NoPressure wear</h1></div>
                    <div class="body">
                        <p>%s</p>
                        <a href="%s" class="button">%s</a>
                        <p style="margin-top: 32px; font-size: 13px; color: #999;">%s</p>
                    </div>
                    <div class="footer"><p>© 2026 NoPressure. %s</p></div>
                </div>
            </body>
            </html>
            """.formatted(
                t.get("verifyText"),
                verifyUrl,
                t.get("verifyButton"),
                t.get("verifyExpire"),
                t.get("allRightsReserved")
            );

        sendHtmlEmail(to, t.get("verifySubject"), html, t.get("allRightsReserved"));
    }

    public void sendPasswordResetEmail(String to, String token, String lang) {
        Map<String, String> t = getEmailTranslations(lang);
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                    .container { max-width: 560px; margin: 40px auto; background: #ffffff; border: 1px solid #e5e5e5; }
                    .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; text-align: center; }
                    .header h1 { margin: 0; font-size: 20px; font-weight: 900; text-transform: uppercase; letter-spacing: -0.5px; color: #111; }
                    .body { padding: 40px; }
                    .body p { font-size: 14px; color: #555; line-height: 1.6; margin: 0 0 24px; }
                    .button { display: inline-block; background: #111; color: #fff !important; text-decoration: none; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; padding: 14px 32px; }
                    .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center;  }
                    .footer p { font-size: 12px; color: #999; margin: 0; }
                    .url { font-size: 12px; color: #999; word-break: break-all; margin-top: 16px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>NoPressure wear</h1>
                    </div>
                    <div class="body">
                        <p>%s</p>
                        <p>%s</p>
                        <a href="%s" class="button">%s</a>
                        <p style="margin-top: 32px; font-size: 13px; color: #999;">%s</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 NoPressure. %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                t.get("resetText"),
                t.get("resetExpire"),
                resetUrl,
                t.get("resetButton"),
                t.get("resetIgnore"),
                t.get("allRightsReserved")
            );

        sendHtmlEmail(to, t.get("resetSubject"), html, t.get("allRightsReserved"));
    }

    public void sendOrderStatusEmail(String to,
                                     Long orderId,
                                     String orderCode,
                                     String status,
                                     String customerFirstName,
                                     String productRows,
                                     String total,
                                     String shippingStreet,
                                     String shippingCity,
                                     String shippingPostalCode,
                                     String shippingCountry,
                                     List<String> productImageUrls,
                                     BigDecimal deliveryFee,
                                     String lang) {

        Map<String, String> t = getEmailTranslations(lang);
        BigDecimal delivery = nonNull(deliveryFee) ? deliveryFee : BigDecimal.ZERO;
        BigDecimal subtotalBD = new BigDecimal(total).subtract(delivery);
        String subtotalDisplay = subtotalBD.toPlainString();
        String deliveryDisplay = delivery.compareTo(BigDecimal.ZERO) == 0
                ? t.get("orderFree")
                : delivery.toPlainString() + " RSD";
        String deliveryStyle = delivery.compareTo(BigDecimal.ZERO) == 0
                ? "color: #16a34a;"
                : "";

        String orderUrl = frontendUrl + "/orders/" + orderId;

        String statusColor = switch (status) {
            case "CONFIRMED" -> "#2563eb";
            case "SHIPPED"   -> "#7c3aed";
            case "DELIVERED" -> "#16a34a";
            case "CANCELLED" -> "#dc2626";
            default          -> "#d97706";
        };
        String statusLabel = t.getOrDefault(status, status);

        String greeting = """
            <p class="status-badge" style="color: #111; margin: 10px 0 10px;">
                %s<br><br>
                %s
                <span style="font-weight: 700; color: %s;">%s</span>.
            </p>
            """.formatted(
                t.get("orderHi").formatted(customerFirstName),
                t.get("orderStatusUpdate").formatted(orderCode),
                statusColor,
                statusLabel
            );

        String shippingSection = (nonNull(shippingStreet) && !shippingStreet.isEmpty()) ? """
            <div style="margin-top: 24px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
                <p style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; color: #111; margin: 0 0 8px;">%s</p>
                <p style="font-size: 13px; color: #555; margin: 0; line-height: 1.6;">%s<br>%s, %s<br>%s</p>
            </div>
            """.formatted(t.get("orderShipping"), shippingStreet, shippingCity, shippingPostalCode, shippingCountry) : "";

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                    .container { max-width: 560px; margin: 40px auto; background: #ffffff; border: 1px solid #e5e5e5; }
                    .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; text-align: center; }
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
                    .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center; }
                    .footer p { font-size: 12px; color: #999; margin: 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>NoPressure wear</h1>
                    </div>
                    <div class="body">
                        <h2 class="order-title">%s #%s</h2>
                        <p class="order-date">%s</p>
                        %s
                        <p class="section-title">%s</p>
                        %s

                        <div style="margin-top: 24px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
                            <div class="summary-row"><span>%s</span><span>%s RSD</span></div>
                            <div class="summary-row"><span>%s</span><span style="%s">%s</span></div>
                            <div class="summary-total"><span>%s</span><span>%s RSD</span></div>
                        </div>

                        %s

                        <a href="%s" class="button">%s</a>
                    </div>
                    <div class="footer">
                        <p>© 2026 NoPressure. %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                statusColor, statusColor,
                t.get("orderTitle"), orderCode,
                t.get("orderStatusUpdatedNote"),
                greeting,
                t.get("orderItems"),
                productRows,
                t.get("orderSubtotal"), subtotalDisplay,
                t.get("orderDelivery"), deliveryStyle, deliveryDisplay,
                t.get("orderTotal"), total,
                shippingSection,
                orderUrl,
                t.get("orderViewButton"),
                t.get("allRightsReserved")
        );

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(t.get("orderSubject").formatted(orderCode, statusLabel));
            helper.setFrom(fromEmail);

            setEmailLogoHeader(html, helper, t.get("allRightsReserved"));

            if (nonNull(productImageUrls)) {
                for (int i = 0; i < productImageUrls.size(); i++) {
                    String rawUrl = productImageUrls.get(i);
                    String relativePath = rawUrl.startsWith("/") ? rawUrl.substring(1) : rawUrl;
                    File imageFile = new File(relativePath);
                    if (imageFile.exists()) {
                        helper.addInline("productImg" + i, new FileSystemResource(imageFile));
                    } else {
                        log.warn("Product image not found for inline embedding: {}", imageFile.getAbsolutePath());
                    }
                }
            }
            attachSignature(helper);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send order status email: " + e.getMessage());
        }
    }

    private void setEmailLogoHeader(String html, MimeMessageHelper helper, String allRightsReserved) throws MessagingException {
        String logoUrl = fetchLogoUrl();
        String withLogo = html.replace("<h1>NoPressure wear</h1>", buildLogoHtml(logoUrl));
        String htmlFinal = injectSignature(withLogo, fetchTagline(), allRightsReserved);
        helper.setText(htmlFinal, true);

        attachLogo(helper, logoUrl);
    }

    private void sendHtmlEmail(String to, String subject, String html, String lang) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            String logoUrl = fetchLogoUrl();
            String tagline = fetchTagline();
            String withLogo = html.replace("<h1>NoPressure wear</h1>", buildLogoHtml(logoUrl));
            String htmlFinal = injectSignature(withLogo, tagline, lang);

            helper.setText(htmlFinal, true);
            attachLogo(helper, logoUrl);
            attachSignature(helper);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String injectSignature(String html, String tagline, String allRightsReserved) {
        String copyrightMarker = """
        <p>© 2026 NoPressure. %s</p>
        """.formatted(allRightsReserved);

        if (html.contains(copyrightMarker)) {
            return html.replace(
                    copyrightMarker,
                    buildSignatureBlock(tagline) +
                            """
                                <p style="font-size: 12px; color: #999; margin: 8px 0 0;">© 2026 NoPressure. %s</p>
                            """.formatted(allRightsReserved)
            );
        }
        String footer = buildSignatureFooter(tagline);
        return html.contains("</body>") ? html.replace("</body>", footer + "</body>") : html + footer;
    }

    private String buildSignatureBlock(String tagline) {
        String taglineHtml = (nonNull(tagline) && !tagline.isBlank())
                ? "<div style=\"font-size: 13px; color: #333; text-align: center; margin-bottom: 16px; line-height: 1.7;\">" + tagline + "</div>"
                : "";
        return taglineHtml + "<img src=\"cid:emailSignature\" alt=\"\" style=\"height: 48px; width: auto; display: block; margin: 0 auto 8px;\" />";
    }

    private String buildSignatureFooter(String tagline) {
        return "<div style=\"text-align: center; margin-top: 40px; padding-top: 32px; border-top: 1px solid #e5e5e5; text-align: center;\">" +
                buildSignatureBlock(tagline) +
                "</div>";
    }

    private String fetchTagline() {
        try {
            return storeSettingsRepository.findByKey("store_tagline")
                    .map(StoreSettings::getValue)
                    .orElse("");
        } catch (Exception e) {
            log.warn("Could not fetch store tagline: {}", e.getMessage());
            return "";
        }
    }

    private String fetchLogoUrl() {
        try {
            return storeSettingsRepository.findByKey("store_logo_url")
                    .map(StoreSettings::getValue)
                    .orElse("");
        } catch (Exception e) {
            log.warn("Could not fetch store logo URL: {}", e.getMessage());
            return "";
        }
    }

    private String buildLogoHtml(String logoUrl) {
        if (logoUrl != null && !logoUrl.isBlank()) {
            return "<img src=\"cid:emailLogo\" alt=\"NoPressure\" style=\"height: 48px; width: auto; display: block; margin: 0 auto;\" />";
        }
        return "<span style=\"font-size: 20px; font-weight: 900; text-transform: uppercase; letter-spacing: -0.5px; color: #111;\">NoPressure wear</span>";
    }

    private void attachLogo(MimeMessageHelper helper, String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) return;
        try {
            String relativePath = logoUrl.startsWith("/") ? logoUrl.substring(1) : logoUrl;
            File logoFile = new File(relativePath);
            if (logoFile.exists()) {
                helper.addInline("emailLogo", new FileSystemResource(logoFile));
            } else {
                log.warn("Logo file not found: {}", logoFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Could not attach logo image: {}", e.getMessage());
        }
    }

    private void attachSignature(MimeMessageHelper helper) {
        try {
            ClassPathResource signature = new ClassPathResource("static/images/signature.png");
            if (signature.exists()) {
                helper.addInline("emailSignature", signature);
            }
        } catch (Exception e) {
            log.warn("Could not attach signature image: {}", e.getMessage());
        }
    }

    public void sendContactEmail(String fromName, String fromEmail, String subject, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(this.fromEmail);
            helper.setSubject("Contact: " + (hasImage(subject) ? subject : "No subject"));
            helper.setFrom(this.fromEmail);
            helper.setReplyTo(fromEmail);

            String html = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 40px 0;">
                <div style="border-bottom: 2px solid #000; padding-bottom: 20px; margin-bottom: 30px;">
                    <h1 style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; text-transform: uppercase; margin: 0; color: #000;">
                        New Contact Message
                    </h1>
                </div>
            
                <div style="background: #f9f9f9; padding: 20px; margin-bottom: 20px;">
                    <p style="font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #999; margin-bottom: 5px;">From</p>
                    <p style="font-size: 14px; color: #333; margin: 0;">%s</p>
                    <p style="font-size: 13px; color: #666; margin: 4px 0 0 0;">%s</p>
                </div>
            
                <div style="background: #f9f9f9; padding: 20px; margin-bottom: 20px;">
                    <p style="font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #999; margin-bottom: 5px;">Subject</p>
                    <p style="font-size: 14px; color: #333; margin: 0;">%s</p>
                </div>

                <div style="border: 1px solid #eee; padding: 20px;">
                    <p style="font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #999; margin-bottom: 10px;">Message</p>
                    <p style="font-size: 14px; color: #333; line-height: 1.6; margin: 0; white-space: pre-line;">%s</p>
                </div>
            </div>
            """.formatted(fromName, fromEmail,
                    hasImage(subject) ? subject : "—",
                    message);

            String footer = buildSignatureFooter(fetchTagline());
            helper.setText(html + footer, true);
            attachSignature(helper);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send contact email: " + e.getMessage());
        }
    }

    public void sendContactConfirmation(String to, String name, String lang) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("sr".equals(lang) ? "Primili smo vašu poruku" : "We received your message");
            helper.setFrom(this.fromEmail);

            String greeting = "sr".equals(lang) ? "Zdravo" : "Hi";
            String body = "sr".equals(lang)
                    ? "Hvala Vam što ste nas kontaktirali. Primili smo vašu poruku i odgovorićemo Vam u najkraćem roku."
                    : "Thank you for reaching out. We received your message and will get back to you as soon as possible.";
            String team = "sr".equals(lang) ? "Vaš tim" : "The Team";

            String html = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 40px 0;">
                <div style="border-bottom: 2px solid #000; padding-bottom: 20px; margin-bottom: 30px;">
                    <h1 style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; text-transform: uppercase; margin: 0; color: #000;">
                        %s
                    </h1>
                </div>
                <p style="font-size: 14px; color: #333;">%s %s,</p>
                <p style="font-size: 14px; color: #555; line-height: 1.6;">%s</p>
                <p style="font-size: 14px; color: #333; margin-top: 30px;">%s</p>
            </div>
            """.formatted(
                    "sr".equals(lang) ? "Hvala Vam!" : "Thank You!",
                    greeting, name, body, team);

            String footer = buildSignatureFooter(fetchTagline());
            helper.setText(html + footer, true);
            attachSignature(helper);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send contact confirmation: " + e.getMessage());
        }
    }

    private Map<String, String> getEmailTranslations(String lang) {
        Map<String, String> t = new HashMap<>();
        if ("sr".equals(lang)) {
            t.put("resetSubject", "Zahtev za resetovanje lozinke");
            t.put("resetTitle", "Resetovanje lozinke");
            t.put("resetHi", "Zdravo");
            t.put("resetText", "Primili smo zahtev za resetovanje vaše lozinke. Kliknite na dugme ispod da biste postavili novu lozinku.");
            t.put("resetButton", "Resetuj lozinku");
            t.put("resetAlt", "Ako dugme ne radi, kopirajte ovaj link u pretraživač:");
            t.put("resetExpire", "Ovaj link ističe za 1 sat.");
            t.put("resetIgnore", "Ako niste tražili resetovanje lozinke, slobodno ignorišite ovaj email.");
            t.put("orderSubject", "Porudžbina #%s — Status: %s");
            t.put("orderTitle", "Ažuriranje porudžbine");
            t.put("orderHi", "Zdravo %s,");
            t.put("orderStatusUpdate", "Status vaše porudžbine <strong>#%s</strong> je ažuriran na:");
            t.put("orderItems", "Stavke porudžbine");
            t.put("orderShipping", "Adresa za dostavu");
            t.put("orderTotal", "Ukupno");
            t.put("orderViewButton", "Pogledaj porudžbinu");
            t.put("orderThankYou", "Hvala Vam na kupovini!");
            t.put("orderQuestions", "Imate pitanja? Kontaktirajte nas.");
            t.put("PENDING", "Na čekanju");
            t.put("CONFIRMED", "Potvrđeno");
            t.put("SHIPPED", "Poslato");
            t.put("DELIVERED", "Dostavljeno");
            t.put("CANCELLED", "Otkazano");
            t.put("orderSubtotal", "Međuzbir");
            t.put("orderDelivery", "Dostava");
            t.put("orderFree", "Besplatno");
            t.put("orderStatusUpdatedNote", "Status ažuriran");
            t.put("allRightsReserved", "Sva prava zadržana.");
            t.put("verifySubject", "Potvrdite vaš email");
            t.put("verifyText", "Dobrodošli! Molimo potvrdite vašu email adresu da biste aktivirali nalog.");
            t.put("verifyButton", "Potvrdi email");
            t.put("verifyExpire", "Link ističe za 24 sata. Ako se niste registrovali, ignorišite ovaj email.");
        } else {
            t.put("resetSubject", "Password Reset Request");
            t.put("resetTitle", "Reset Your Password");
            t.put("resetHi", "Hi");
            t.put("resetText", "We received a request to reset your password. Click the button below to set a new password.");
            t.put("resetButton", "Reset Password");
            t.put("resetAlt", "If the button doesn't work, copy this link into your browser:");
            t.put("resetExpire", "This link will expire in 1 hour.");
            t.put("resetIgnore", "If you didn't request a password reset, feel free to ignore this email.");
            t.put("orderSubject", "Order #%s — Status: %s");
            t.put("orderTitle", "Order Update");
            t.put("orderHi", "Hi %s,");
            t.put("orderStatusUpdate", "Your order <strong>#%s</strong> status has been updated to:");
            t.put("orderItems", "Order Items");
            t.put("orderShipping", "Shipping Address");
            t.put("orderTotal", "Total");
            t.put("orderViewButton", "View Order");
            t.put("orderThankYou", "Thank you for your purchase!");
            t.put("orderQuestions", "Have questions? Contact us.");
            t.put("PENDING", "Pending");
            t.put("CONFIRMED", "Confirmed");
            t.put("SHIPPED", "Shipped");
            t.put("DELIVERED", "Delivered");
            t.put("CANCELLED", "Cancelled");
            t.put("orderSubtotal", "Subtotal");
            t.put("orderDelivery", "Delivery");
            t.put("orderFree", "Free");
            t.put("orderStatusUpdatedNote", "Status updated");
            t.put("allRightsReserved", "All rights reserved.");
            t.put("verifySubject", "Verify your email");
            t.put("verifyText", "Welcome! Please confirm your email address to activate your account.");
            t.put("verifyButton", "Verify Email");
            t.put("verifyExpire", "This link expires in 24 hours. If you didn't sign up, ignore this email.");
        }
        return t;
    }

    public void sendNotificationEmail(String to,
                                      String subject,
                                      String message,
                                      String imageUrl,
                                      String bgColor,
                                      String textColor,
                                      String lang) {
        try {
            Map<String, String> t = getEmailTranslations(lang);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(hasImage(subject) ? subject : "Special Offer");
            helper.setFrom(fromEmail);

            String bg = (bgColor != null && !bgColor.isBlank()) ? bgColor : "#ffffff";
            String text = (textColor != null && !textColor.isBlank()) ? textColor : "#111111";

            String imageHtml = hasImage(imageUrl) ? """
                    <div style="margin: 0 0 24px;">
                        <img src="cid:notificationImage" alt="" style="width: 100%; max-width: 100%; height: auto; display: block;" />
                    </div>
                    """ : "";

            String subjectHtml = hasImage(subject) ? """
                    <h2 style="font-size: 20px; font-weight: 800; text-transform: uppercase; letter-spacing: -0.5px; margin: 0 0 20px; color: %s;">%s</h2>
                    """.formatted(text, subject) : "";

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                            .container { max-width: 560px; margin: 40px auto; background: %s; border: 1px solid #e5e5e5; }
                            .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; text-align: center; }
                            .body { padding: 40px; }
                            .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center; }
                            .footer p { font-size: 12px; color: #999; margin: 0; }
                            .broadcast-content p { margin: 0 0 10px; }
                            .broadcast-content ul { padding-left: 20px; margin: 0 0 10px; }
                            .broadcast-content ol { padding-left: 20px; margin: 0 0 10px; }
                            .broadcast-content strong { font-weight: 700; }
                            .broadcast-content em { font-style: italic; }
                            .broadcast-content h1, .broadcast-content h2, .broadcast-content h3 { margin: 0 0 8px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>NoPressure wear</h1>
                            </div>
                            <div class="body">
                                %s
                                %s
                                <div class="broadcast-content" style="font-size: 14px; color: %s; line-height: 1.6;">
                                    %s
                                </div>
                            </div>
                            <div class="footer">
                                <p>© 2026 NoPressure. %s</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(bg, imageHtml, subjectHtml, text, message, t.get("allRightsReserved"));

            setEmailLogoHeader(html, helper, t.get("allRightsReserved"));

            if (hasImage(imageUrl)) {
                String relativePath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
                File imageFile = new File(relativePath);
                if (imageFile.exists()) {
                    helper.addInline("notificationImage", new FileSystemResource(imageFile));
                } else {
                    log.warn("Notification image file not found: {}", imageFile.getAbsolutePath());
                }
            }

            attachSignature(helper);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", to, e.getMessage());
        }
    }

    private static boolean hasImage(String imageUrl) {
        return nonNull(imageUrl) && !imageUrl.isBlank();
    }
}