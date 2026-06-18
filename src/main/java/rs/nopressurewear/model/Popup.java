package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "popup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Popup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private String mediaUrl;

    @Column
    private String mediaType;

    @Column
    private String buttonText;

    @Column
    private String buttonLink;

    @Column
    private String backgroundColor;

    @Column
    private String textColor;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean showOnce = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}