package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.model.Cart;
import rs.nopressurewear.repository.CartRepository;
import rs.nopressurewear.repository.StoreSettingsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbandonedCartService {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartService.class);

    private final CartRepository cartRepository;
    private final EmailService emailService;
    private final StoreSettingsRepository settingsRepository;

    @Value("${cart.abandoned.enabled:true}")
    private boolean enabled;

    @Value("${cart.abandoned.after-hours:4}")
    private int afterHours;

    @Value("${cart.abandoned.after-minutes:15}")
    private int afterMinutes;

    @Value("${cart.abandoned.max-age-hours:72}")
    private int maxAgeHours;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendReminders() {
        if (!enabled) return;

        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime idleBefore = now.minusHours(afterHours);
        LocalDateTime idleBefore = now.minusMinutes(afterMinutes);
        LocalDateTime notOlderThan = now.minusHours(maxAgeHours);

        List<Cart> carts = cartRepository.findAbandonedCarts(idleBefore, notOlderThan);
        String lang = defaultLanguage();

        for (Cart cart : carts) {
            try {
                emailService.sendAbandonedCartEmail(
                        cart.getUser().getEmail(),
                        cart.getUser().getFirstName(),
                        cart.getCartItems(),
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
        return settingsRepository.findByKey("default_language")
                .map(s -> s.getValue())
                .orElse("en");
    }
}
