I want to append the signature image + a short signature line at the bottom of every email, like the product description 
footer. Since the signature is a local image (not a public URL), embed it inline via CID so it renders in every email client.
Backend — EmailService.java

You have a central sendHtmlEmail(to, subject, html) method that all emails go through. The cleanest approach: build the 
footer HTML once and attach the signature image inline in sendHtmlEmail, so every email gets it automatically without 
changing each individual email method.

First, place the signature image somewhere the backend can read it — e.g. src/main/resources/static/images/signature.png 
(it'll be on the classpath).

Add a constant for the footer and update sendHtmlEmail:

import org.springframework.core.io.ClassPathResource;
<java code>
private static final String SIGNATURE_FOOTER = """
<div style="text-align: center; margin-top: 40px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
<img src="cid:emailSignature" alt="" style="height: 48px; width: auto; display: inline-block;" />
</div>
""";

private void sendHtmlEmail(String to, String subject, String html) {
try {
MimeMessage message = mailSender.createMimeMessage();
MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(fromEmail);
        helper.setText(html, true);

        // Inline signature image (CID) — renders in all email clients
        ClassPathResource signature = new ClassPathResource("static/images/signature.png");
        if (signature.exists()) {
            helper.addInline("emailSignature", signature);
        }

        mailSender.send(message);
    } catch (Exception e) {
        log.error("Failed to send email: " + e.getMessage());
    }
}
</java code>

Now inject SIGNATURE_FOOTER into your email HTML. The simplest place is right before the closing .footer or before </div>
of the body, in each template. In sendOrderStatusEmail, add it after the "View Order" button, before the footer div:

<a href="%s" class="button">View Order</a>

%s

<div class="footer">
    <p>© 2026 NoPressure. All rights reserved.</p>
</div>

and add SIGNATURE_FOOTER to the .formatted(...) arguments at the matching %s position:

.formatted(
statusColor, statusColor,
orderCode,
greeting,
productRows,
subtotal, subtotal,
shippingSection,
orderUrl,
SIGNATURE_FOOTER   // ← new %s slot
)

To make it apply to every email without editing each template, the better approach is to inject it centrally in 
sendHtmlEmail — wrap or insert the footer just before </body>:

private void sendHtmlEmail(String to, String subject, String html) {
try {
MimeMessage message = mailSender.createMimeMessage();
MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(fromEmail);

        // Insert the signature footer before </body> so it appears in every email
        String htmlWithFooter = html.contains("</body>")
                ? html.replace("</body>", SIGNATURE_FOOTER + "</body>")
                : html + SIGNATURE_FOOTER;

        helper.setText(htmlWithFooter, true);

        ClassPathResource signature = new ClassPathResource("static/images/signature.png");
        if (signature.exists()) {
            helper.addInline("emailSignature", signature);
        }

        mailSender.send(message);
    } catch (Exception e) {
        log.error("Failed to send email: " + e.getMessage());
    }
}

This is cleaner — you don't touch any individual email method. Every email routed through sendHtmlEmail gets the 
signature inserted before </body> and the inline image attached. The cid:emailSignature reference matches the 
addInline("emailSignature", ...).

If you want the text from your second screenshot too (not just the signature image), that text is product-specific 
(it describes the NoPressure Circle product), so it shouldn't go in every email footer. But if you want a generic tagline 
under the signature, add it to SIGNATURE_FOOTER:

private static final String SIGNATURE_FOOTER = """
<div style="text-align: center; margin-top: 40px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
<img src="cid:emailSignature" alt="" style="height: 48px; width: auto; display: inline-block;" />
<p style="font-size: 12px; color: #999; margin: 12px 0 0; font-style: italic;">No pressure. Just style.</p>
</div>
""";

A note on why CID and not a URL: your signature is a local asset, and localhost URLs don't load in email clients. 
Embedding it inline as a CID attachment means the image bytes travel with the email, so it renders everywhere — Mailtrap, 
Gmail, Outlook, all of them. Putting the file in src/main/resources/static/images/signature.png makes it available via 
ClassPathResource at runtime.

Drop the nopressure_signature_footer.png into src/main/resources/static/images/signature.png, apply the central 
sendHtmlEmail change, and every email will carry the signature.
