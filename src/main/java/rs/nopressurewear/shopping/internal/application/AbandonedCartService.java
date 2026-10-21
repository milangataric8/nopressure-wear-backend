package rs.nopressurewear.shopping.internal.application;
import rs.nopressurewear.notifications.api.EmailOperations;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.shopping.internal.domain.Cart;
import rs.nopressurewear.shopping.internal.persistence.CartRepository;
import rs.nopressurewear.storefront.api.SettingsQuery;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbandonedCartService {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartService.class);

    private final CartRepository cartRepository;
    private final EmailOperations emailService;
    private final SettingsQuery settingsRepository;

    @Value("${cart.abandoned.enabled:true}")
    private boolean enabled;

    @Value("${cart.abandoned.after-hours:4}")
    private int afterHours;

    @Value("${cart.abandoned.max-age-hours:72}")
    private int maxAgeHours;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendReminders() {
        if (!enabled) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime idleBefore = now.minusHours(afterHours);
        LocalDateTime notOlderThan = now.minusHours(maxAgeHours);

        List<Cart> carts = cartRepository.findAbandonedCarts(idleBefore, notOlderThan);
        String lang = defaultLanguage();

        for (Cart cart : carts) {
            try {
                emailService.sendAbandonedCartEmail(
                        cart.getUser().getEmail(),
                        cart.getUser().getFirstName(),
                        cart.getCartItems().stream().map(item -> new rs.nopressurewear.notifications.api.CartReminderItem(
                                item.getProduct() != null ? item.getProduct().getName() : "—",
                                item.getSize() != null ? item.getSize().name() : null,
                                item.getProduct() != null ? item.getProduct().getImageUrl() : null,
                                item.getProduct() != null ? item.getProduct().getPrice() : null,
                                item.getQuantity())).toList(),
                        lang
                );
                cart.setReminderSentAt(now);
                cartRepository.save(cart);
            } catch (Exception e) {
                log.warn("Failed to send abandoned cart reminder for cart {}: {}", cart.getId(), e.getMessage());
            }
        }

        if (!carts.isEmpty()) {
            log.info("Sent {} abandoned cart reminder(s)", carts.size());
        }
    }

    private String defaultLanguage() {
        return settingsRepository.findValue("default_language")
                .orElse("en");
    }
}
