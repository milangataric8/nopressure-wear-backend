package rs.webshop.webshop_core.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_location_id", nullable = false)
    private StoreLocation storeLocation;

    @Column(nullable = false)
    private boolean inStock = true;
}