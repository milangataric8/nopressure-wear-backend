package rs.nopressurewear.notifications.internal.channel;

public interface NotificationChannel {
    String getName();
    boolean isEnabled();
    void send(String recipient, String subject, String message, String imageUrl, String bgColor, String textColor, String lang);
}