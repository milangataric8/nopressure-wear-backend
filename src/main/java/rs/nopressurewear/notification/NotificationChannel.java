package rs.nopressurewear.notification;

public interface NotificationChannel {
    String getName();
    boolean isEnabled();
    void send(String recipient, String subject, String message, String imageUrl);
}