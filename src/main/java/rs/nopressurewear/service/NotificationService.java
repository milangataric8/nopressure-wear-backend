package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.model.Notification;
import rs.nopressurewear.model.User;
import rs.nopressurewear.notification.NotificationChannel;
import rs.nopressurewear.repository.NotificationRepository;
import rs.nopressurewear.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationChannel> channels;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> notification(rs.nopressurewear.dto.notification.NotificationRequest request, String sentBy) {
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
                        if (nonNull(customer.getEmail())) {
                            channel.send(customer.getEmail(), request.getSubject(), request.getMessage(), request.getImageUrl(), request.getBgColor(), request.getTextColor());
                            emailCount++;
                        }
                    }
                    case "WHATSAPP" -> {
                        if (nonNull(customer.getPhone())) {
                            channel.send(customer.getPhone(), request.getSubject(), request.getMessage(), request.getImageUrl(), null, null);
                            whatsappCount++;
                        }
                    }
                    case "VIBER" -> {
                        if (nonNull(customer.getPhone())) {
                            channel.send(customer.getPhone(), request.getSubject(), request.getMessage(), request.getImageUrl(), null, null);
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
                .bgColor(request.getBgColor())
                .textColor(request.getTextColor())
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