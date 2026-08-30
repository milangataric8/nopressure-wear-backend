package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;
import rs.nopressurewear.constants.Gender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.math.BigDecimal.ZERO;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String sku;

    @Column
    private String imageUrl;

    @Column
    private String colorName;

    @Column
    private String colorHex;

    @Column
    private String videoUrl;

    @OneToMany(mappedBy = "product", cascade = ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String brand;

    @Column(precision = 2, scale = 1)
    private BigDecimal averageRating = ZERO;

    @Column
    private Integer ratingCount = 0;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage = ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column
    private String material;

    @Column
    private Integer salesCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender = Gender.UNISEX;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
