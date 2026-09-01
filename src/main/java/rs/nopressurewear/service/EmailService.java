package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.nopressurewear.model.StoreSettings;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.service.email.EmailSender;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String NOPRESSURE_EMAIL = "nopressure.wear.official@gmail.com";

    private final EmailSender emailSender;
    private final StoreSettingsRepository storeSettingsRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.base-url}")
    private String baseUrl;

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
                    <div class="footer">%s</div>
                </div>
            </body>
            </html>
            """.formatted(
                t.get("verifyText"),
                verifyUrl,
                t.get("verifyButton"),
                t.get("verifyExpire"),
                copyrightLine(t.get("allRightsReserved"))
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
                        %s
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
                copyrightLine(t.get("allRightsReserved"))
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
                                     BigDecimal deliveryFee,
                                     String lang) {

        Map<String, String> t = getEmailTranslations(lang);
        BigDecimal delivery = nonNull(deliveryFee) ? deliveryFee : ZERO;
        BigDecimal subtotalBD = new BigDecimal(total).subtract(delivery);
        String subtotalDisplay = subtotalBD.toPlainString();
        String deliveryDisplay = delivery.compareTo(ZERO) == 0
                ? t.get("orderFree")
                : delivery.toPlainString() + " RSD";
        String deliveryStyle = delivery.compareTo(ZERO) == 0
                ? "color: #16a34a;"
                : "";

        String orderUrl = frontendUrl + "/orders/" + orderId;

        // Order summary as a two-cell table (label left, amount right). No flexbox/float —
        // Outlook's Word engine drops both, which is why the amounts used to stick to the
        // label on the left. align="right" is set as an HTML attribute as well as CSS
        // because some Outlook builds ignore text-align on <td>.
        String summaryTable = """
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="width:100%%; border-collapse:collapse;">
                <tr>
                    <td width="60%%" align="left" style="text-align:left; padding:6px 0; color:#555; font-size:14px;">%s</td>
                    <td width="40%%" align="right" style="text-align:right; padding:6px 0; color:#555; font-size:14px; white-space:nowrap;">%s RSD</td>
                </tr>
                <tr>
                    <td align="left" style="text-align:left; padding:6px 0; color:#555; font-size:14px;">%s</td>
                    <td align="right" style="text-align:right; padding:6px 0; color:#555; font-size:14px; white-space:nowrap; %s">%s</td>
                </tr>
                <tr>
                    <td colspan="2" style="border-top:1px solid #e5e5e5; font-size:0; line-height:0; height:1px;">&nbsp;</td>
                </tr>
                <tr>
                    <td align="left" style="text-align:left; padding:10px 0; font-weight:bold; font-size:16px; color:#111;">%s</td>
                    <td align="right" style="text-align:right; padding:10px 0; font-weight:bold; font-size:16px; color:#111; white-space:nowrap;">%s RSD</td>
                </tr>
            </table>
            """.formatted(
                t.get("orderSubtotal"), subtotalDisplay,
                t.get("orderDelivery"), deliveryStyle, deliveryDisplay,
                t.get("orderTotal"), total
            );

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
                    .item-img { width: 56px; height: 56px; background: #f5f5f5; object-fit: contain; }
                    .item-name { font-size: 13px; font-weight: 600; color: #111; margin: 0 0 4px; }
                    .item-qty { font-size: 12px; color: #999; margin: 0; }
                    .item-price { font-size: 13px; font-weight: 700; color: #111; }
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
                            %s
                        </div>

                        %s

                        <a href="%s" class="button">%s</a>
                    </div>
                    <div class="footer">
                        %s
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
                summaryTable,
                shippingSection,
                orderUrl,
                t.get("orderViewButton"),
                copyrightLine(t.get("allRightsReserved"))
        );

        String subject = t.get("orderSubject").formatted(orderCode, statusLabel);
        emailSender.send(to, subject, applyLogoAndSignature(html, t.get("allRightsReserved")));
    }

    /**
     * Resolves a possibly-relative image path (as stored on products/settings, e.g.
     * {@code /uploads/products/xyz.png}) into an absolute URL. Already-absolute URLs
     * (e.g. Cloudinary's {@code secure_url} in production) are returned unchanged.
     * Every image referenced in an email must be an absolute URL — the Brevo API has
     * no support for {@code cid:} inline attachments.
     */
    public String resolveImageUrl(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return null;
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) return urlOrPath;
        return baseUrl + (urlOrPath.startsWith("/") ? urlOrPath : "/" + urlOrPath);
    }

    /** Swaps the plain "NoPressure wear" header for the store logo (if configured) and appends the signature block. */
    private String applyLogoAndSignature(String html, String allRightsReserved) {
        String logoUrl = fetchLogoUrl();
        String tagline = fetchTagline();
        String withLogo = html.replace("<h1>NoPressure wear</h1>", buildLogoHtml(logoUrl));
        return injectSignature(withLogo, tagline, allRightsReserved);
    }

    private void sendHtmlEmail(String to, String subject, String html, String allRightsReserved) {
        emailSender.send(to, subject, applyLogoAndSignature(html, allRightsReserved));
    }

    /** The "© 2026 NoPressure. {rights text}" line every template's footer ends with. */
    private String copyrightLine(String allRightsReserved) {
        return "<p>© 2026 NoPressure. " + allRightsReserved + "</p>";
    }

    private String injectSignature(String html, String tagline, String allRightsReserved) {
        String copyrightMarker = copyrightLine(allRightsReserved);

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
        return taglineHtml + "<img src=\"" + baseUrl + "/images/signature.png\" alt=\"\" style=\"height: 48px; width: auto; display: block; margin: 0 auto 8px;\" />";
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
        String resolved = resolveImageUrl(logoUrl);
        if (nonNull(resolved)) {
            return "<img src=\"" + resolved + "\" alt=\"NoPressure\" style=\"height: 48px; width: auto; display: block; margin: 0 auto;\" />";
        }
        return "<span style=\"font-size: 20px; font-weight: 900; text-transform: uppercase; letter-spacing: -0.5px; color: #111;\">NoPressure wear</span>";
    }

    public void sendAdminLowStockAlert(String productName, String sku, String colorName, String gender,
                                      String size, int stock, int threshold, String lang) {
        Map<String, String> t = getEmailTranslations(lang);
        String detailRowTemplate = "<tr><td style=\"color:#999;padding:4px 0;\">%s</td><td style=\"padding:4px 0;\"><strong>%s</strong></td></tr>";
        String colorRow = nonNull(colorName) && !colorName.isBlank()
                ? detailRowTemplate.formatted(t.get("lowStockColor"), colorName)
                : "";
        String genderRow = nonNull(gender) && !gender.isBlank()
                ? detailRowTemplate.formatted(t.get("lowStockGender"), gender)
                : "";
        String sizeRow = nonNull(size)
                ? detailRowTemplate.formatted(t.get("lowStockSize"), size)
                : "";
        String identityRows = colorRow + genderRow + sizeRow;
        String subject = t.get("lowStockSubject").formatted(productName);
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
                        .badge { display: inline-block; background: #111; color: #fff; font-size: 24px; font-weight: 900; padding: 12px 24px; margin: 0 0 24px; }
                        table { border-collapse: collapse; font-size: 14px; color: #555; }
                        td { padding: 4px 16px 4px 0; vertical-align: top; }
                        .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center; }
                        .footer p { font-size: 12px; color: #999; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header"><h1>NoPressure wear</h1></div>
                        <div class="body">
                            <p>%s</p>
                            <div class="badge">%d</div>
                            <table>
                                %s
                                <tr><td style="color:#999;padding:4px 0;">%s</td><td style="padding:4px 0;"><strong>%s</strong></td></tr>
                                <tr><td style="color:#999;padding:4px 0;">%s</td><td style="padding:4px 0;">%d</td></tr>
                            </table>
                            <p style="margin-top:24px;">%s</p>
                        </div>
                        <div class="footer">%s</div>
                    </div>
                </body>
                </html>
                """.formatted(
                t.get("lowStockIntro").formatted(productName),
                stock,
                identityRows,
                t.get("lowStockSku"), sku,
                t.get("lowStockThresholdLabel"), threshold,
                t.get("lowStockRestock"),
                copyrightLine(t.get("allRightsReserved"))
        );
        emailSender.send(NOPRESSURE_EMAIL, subject, applyLogoAndSignature(html, t.get("allRightsReserved")));
    }

    public void sendAbandonedCartEmail(String to, String firstName, List<rs.nopressurewear.model.CartItem> items, String lang) {
        Map<String, String> t = getEmailTranslations(lang);
        String cartUrl = frontendUrl + "/cart";

        StringBuilder itemRows = new StringBuilder();
        for (rs.nopressurewear.model.CartItem item : items) {
            String name = item.getProduct() != null ? item.getProduct().getName() : "—";
            String sizeLabel = item.getSize() != null ? " &mdash; " + item.getSize() : "";
            String imageUrl = item.getProduct() != null ? item.getProduct().getImageUrl() : null;
            String resolvedImageUrl = resolveImageUrl(imageUrl);
            String imgHtml = nonNull(resolvedImageUrl)
                    ? "<img src=\"" + resolvedImageUrl + "\" alt=\"\" width=\"56\" height=\"56\" style=\"object-fit:cover;border:1px solid #e5e5e5;display:block;\" />"
                    : "<div style=\"width:56px;height:56px;background:#f5f5f5;\"></div>";
            String price = item.getProduct() != null ? item.getProduct().getPrice().toPlainString() : "—";
            itemRows.append("""
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="width:100%%;border-collapse:collapse;border-bottom:1px solid #f0f0f0;">
                        <tr>
                            <td width="56" style="padding:12px 16px 12px 0;vertical-align:top;">%s</td>
                            <td align="left" style="text-align:left;padding:12px 0;vertical-align:top;">
                                <p style="margin:0 0 4px;font-size:14px;font-weight:600;color:#111;">%s%s</p>
                                <p style="margin:0;font-size:13px;color:#999;">%s: %d &times; %s RSD</p>
                            </td>
                        </tr>
                    </table>
                    """.formatted(imgHtml, name, sizeLabel, t.get("cartQty"), item.getQuantity(), price));
        }

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
                        .body p { font-size: 14px; color: #555; line-height: 1.6; margin: 0 0 16px; }
                        .button { display: inline-block; background: #111; color: #fff !important; text-decoration: none; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; padding: 14px 32px; margin-top: 24px; }
                        .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center; }
                        .footer p { font-size: 12px; color: #999; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header"><h1>NoPressure wear</h1></div>
                        <div class="body">
                            <p>%s</p>
                            <p>%s</p>
                            <div style="margin: 24px 0;">%s</div>
                            <a href="%s" class="button">%s</a>
                        </div>
                        <div class="footer">
                            %s
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                t.get("cartHi").formatted(firstName),
                t.get("cartReminder"),
                itemRows,
                cartUrl,
                t.get("cartButton"),
                copyrightLine(t.get("allRightsReserved"))
        );

        emailSender.send(to, t.get("cartSubject"), applyLogoAndSignature(html, t.get("allRightsReserved")));
    }

    public void sendContactEmail(String fromName, String fromEmail, String subject, String message) {
        try {
            String allRightsReserved = "All rights reserved.";
            String html = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 40px 0;">
                <div style="text-align: center; padding-bottom: 24px; margin-bottom: 30px; border-bottom: 2px solid #000;">
                    <h1>NoPressure wear</h1>
                </div>
                <h2 style="font-size: 20px; font-weight: 800; letter-spacing: -0.5px; text-transform: uppercase; margin: 0 0 24px; color: #000;">
                    New Contact Message
                </h2>

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

                <div style="text-align: center; margin-top: 40px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
                    %s
                </div>
            </div>
            """.formatted(fromName, fromEmail,
                    hasImage(subject) ? subject : "—",
                    message,
                    copyrightLine(allRightsReserved));

            String emailSubject = "Contact: " + (hasImage(subject) ? subject : "No subject");
            emailSender.send(NOPRESSURE_EMAIL, emailSubject, applyLogoAndSignature(html, allRightsReserved), fromEmail);
        } catch (Exception e) {
            log.error("Failed to send contact email: " + e.getMessage());
        }
    }

    public void sendContactConfirmation(String to, String name, String lang) {
        try {
            Map<String, String> t = getEmailTranslations(lang);
            String greeting = "sr".equals(lang) ? "Zdravo" : "Hi";
            String body = "sr".equals(lang)
                    ? "Hvala Vam što ste nas kontaktirali. Primili smo vašu poruku i odgovorićemo Vam u najkraćem roku."
                    : "Thank you for reaching out. We received your message and will get back to you as soon as possible.";
            String team = "sr".equals(lang) ? "Vaš tim" : "The Team";

            String html = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 40px 0;">
                <div style="text-align: center; padding-bottom: 24px; margin-bottom: 30px; border-bottom: 2px solid #000;">
                    <h1>NoPressure wear</h1>
                </div>
                <h2 style="font-size: 20px; font-weight: 800; letter-spacing: -0.5px; text-transform: uppercase; margin: 0 0 24px; color: #000;">
                    %s
                </h2>
                <p style="font-size: 14px; color: #333;">%s %s,</p>
                <p style="font-size: 14px; color: #555; line-height: 1.6;">%s</p>
                <p style="font-size: 14px; color: #333; margin-top: 30px;">%s</p>

                <div style="text-align: center; margin-top: 40px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
                    %s
                </div>
            </div>
            """.formatted(
                    "sr".equals(lang) ? "Hvala Vam!" : "Thank You!",
                    greeting, name, body, team,
                    copyrightLine(t.get("allRightsReserved")));

            String subject = "sr".equals(lang) ? "Primili smo vašu poruku" : "We received your message";
            emailSender.send(to, subject, applyLogoAndSignature(html, t.get("allRightsReserved")));
        } catch (Exception e) {
            log.error("Failed to send contact confirmation: " + e.getMessage());
        }
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

            String bg = (nonNull(bgColor) && !bgColor.isBlank()) ? bgColor : "#ffffff";
            String text = (nonNull(textColor) && !textColor.isBlank()) ? textColor : "#111111";

            String resolvedImageUrl = resolveImageUrl(imageUrl);
            String imageHtml = nonNull(resolvedImageUrl) ? """
                    <div style="margin: 0 0 24px;">
                        <img src="%s" alt="" style="width: 100%%; max-width: 100%%; height: auto; display: block;" />
                    </div>
                    """.formatted(resolvedImageUrl) : "";

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
                                %s
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(bg, imageHtml, subjectHtml, text, message, copyrightLine(t.get("allRightsReserved")));

            emailSender.send(to, hasImage(subject) ? subject : "Special Offer", applyLogoAndSignature(html, t.get("allRightsReserved")));
        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", to, e.getMessage());
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
            t.put("lowStockSubject", "Upozorenje o zalihama: %s");
            t.put("lowStockIntro", "Zalihe za proizvod <strong>%s</strong> su dostigle kritičan nivo.");
            t.put("lowStockColor", "Boja");
            t.put("lowStockGender", "Pol");
            t.put("lowStockSize", "Veličina");
            t.put("lowStockSku", "SKU");
            t.put("lowStockThresholdLabel", "Prag upozorenja ");
            t.put("lowStockRestock", "Razmotrite dopunu zaliha što pre.");
            t.put("cartSubject", "Ostavili ste nešto u korpi");
            t.put("cartHi", "Zdravo %s,");
            t.put("cartReminder", "Još uvek imate artikle u korpi. Završite porudžbinu pre nego što nestanu.");
            t.put("cartButton", "Nazad u korpu");
            t.put("cartQty", "Kom");
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
            t.put("lowStockSubject", "Low stock alert: %s");
            t.put("lowStockIntro", "Stock for <strong>%s</strong> has reached a critical level.");
            t.put("lowStockColor", "Color");
            t.put("lowStockGender", "Gender");
            t.put("lowStockSize", "Size");
            t.put("lowStockSku", "SKU");
            t.put("lowStockThresholdLabel", "Alert threshold ");
            t.put("lowStockRestock", "Consider restocking soon.");
            t.put("cartSubject", "You left something behind");
            t.put("cartHi", "Hi %s,");
            t.put("cartReminder", "You still have items waiting in your cart. Complete your order before they're gone.");
            t.put("cartButton", "Return to cart");
            t.put("cartQty", "Qty");
        }
        return t;
    }

    private static boolean hasImage(String imageUrl) {
        return nonNull(imageUrl) && !imageUrl.isBlank();
    }
}
