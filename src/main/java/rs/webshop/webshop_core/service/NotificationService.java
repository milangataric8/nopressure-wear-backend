package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.model.Notification;
import rs.webshop.webshop_core.model.User;
import rs.webshop.webshop_core.notification.NotificationChannel;
import rs.webshop.webshop_core.repository.NotificationRepository;
import rs.webshop.webshop_core.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationChannel> channels;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> notification(rs.webshop.webshop_core.dto.notification.NotificationRequest request, String sentBy) {
        List<User> customers = userRepository.findByRoleAndIsActiveTrue("CUSTOMER");

        Map<String, NotificationChannel> channelMap = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getName, c -> c));

        int emailCount = 0, whatsappCount = 0, viberCount = 0;

        for (User customer : customers) {
            for (String channelName : request.getChannels()) {
                NotificationChannel channel = channelMap.get(channelName);
                if (channel == null || !channel.isEnabled()) continue;

                switch (channelName) {
                    case "EMAIL" -> {
                        if (customer.getEmail() != null) {
                            channel.send(customer.getEmail(), request.getSubject(), request.getMessage(), request.getImageUrl());
                            emailCount++;
                        }
                    }
                    case "WHATSAPP" -> {
                        if (customer.getPhone() != null) {
                            channel.send(customer.getPhone(), request.getSubject(), request.getMessage(), request.getImageUrl());
                            whatsappCount++;
                        }
                    }
                    case "VIBER" -> {
                        if (customer.getPhone() != null) {
                            channel.send(customer.getPhone(), request.getSubject(), request.getMessage(), request.getImageUrl());
                            viberCount++;
                        }
                    }
                }
            }
        }

        Notification notification = Notification.builder()
                .subject(request.getSubject())
                .message(request.getMessage())
                .imageUrl(request.getImageUrl())
                .channels(String.join(", ", request.getChannels()))
                .recipients(customers.size())
                .sentBy(sentBy)
                .build();
        notificationRepository.save(notification);

        return Map.of(
                "totalCustomers", customers.size(),
                "emailSent", emailCount
//                "whatsappSent", whatsappCount,
//                "viberSent", viberCount
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Notification> getHistory() {
        return notificationRepository.findAllByOrderBySentAtDesc();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> getChannelStatus() {
        return channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getName, NotificationChannel::isEnabled));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}