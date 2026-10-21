package rs.nopressurewear.notifications.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Synchronous notification contract. Delivery and retry behavior stay in this module. */
public interface EmailOperations {
    void sendVerificationEmail(String to, String token, Locale locale);

    void sendPasswordResetEmail(String to, String token, String lang);

    void sendOrderStatusEmail(String to,
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
                                     Locale locale);

    String resolveImageUrl(String urlOrPath);

    void sendAdminLowStockAlert(String productName, String sku, String colorName, String gender,
                                      String size, int stock, int threshold, String lang);

    void sendSuperAdminRoleGrantAlert(String to, String targetEmail, String actorEmail, LocalDateTime when, String lang);

    void sendAbandonedCartEmail(String to, String firstName, List<rs.nopressurewear.notifications.api.CartReminderItem> items, String lang);

    void sendContactEmail(String fromName, String fromEmail, String subject, String message);

    void sendContactConfirmation(String to, String name, String lang);

    void sendNotificationEmail(String to,
                                      String subject,
                                      String message,
                                      String imageUrl,
                                      String bgColor,
                                      String textColor,
                                      String lang);
}
