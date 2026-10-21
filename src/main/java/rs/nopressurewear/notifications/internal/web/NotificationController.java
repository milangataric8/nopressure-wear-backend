package rs.nopressurewear.notifications.internal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.notifications.internal.domain.Notification;
import rs.nopressurewear.notifications.internal.application.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notification")
    public ResponseEntity<Map<String, Object>> notification(
            @Valid @RequestBody rs.nopressurewear.notifications.api.dto.notification.NotificationRequest request,
            @RequestParam(defaultValue = "en") String lang) {
        String sentBy = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(notificationService.notification(request, sentBy, lang));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Notification>> history() {
        return ResponseEntity.ok(notificationService.getHistory());
    }

    @GetMapping("/channels")
    public ResponseEntity<Map<String, Boolean>> channelStatus() {
        return ResponseEntity.ok(notificationService.getChannelStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}