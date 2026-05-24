package rs.webshop.webshop_core.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "filter_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fieldName;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String filterType;

    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;
}