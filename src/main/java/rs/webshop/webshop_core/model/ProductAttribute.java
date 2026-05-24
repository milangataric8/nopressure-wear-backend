package rs.webshop.webshop_core.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attribute")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_key", nullable = false)
    private String key;

    @Column(name = "attribute_value", nullable = false)
    private String value;
}