package rs.nopressurewear.notifications.api;

import java.math.BigDecimal;

/** Snapshot prepared by shopping while its cart transaction is still open. */
public record CartReminderItem(String name, String size, String imageUrl, BigDecimal price, int quantity) {
}
