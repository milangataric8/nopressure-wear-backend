package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "product_variant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private ProductSize size;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "sku")
    private String sku;

    @Builder.Default
    @Column(name = "low_stock_alerted", nullable = false)
    private boolean lowStockAlerted = false;
}
