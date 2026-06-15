package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.model.Notification;
import rs.webshop.webshop_core.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notification")
    public ResponseEntity<Map<String, Object>> notification(
            @Valid @RequestBody rs.webshop.webshop_core.dto.notification.NotificationRequest request) {
        String sentBy = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(notificationService.notification(request, sentBy));
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