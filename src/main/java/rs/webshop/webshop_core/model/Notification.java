package rs.webshop.webshop_core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "notification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private String channels;

    private Integer recipients;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private String sentBy;

    @PrePersist
    void onCreate() { sentAt = LocalDateTime.now(); }
}