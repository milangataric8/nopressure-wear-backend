package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;
import rs.nopressurewear.constants.MediaType;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "banner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column
    private String subtitle;

    @Column
    private String mediaUrl;

    @Enumerated(STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column
    private String buttonText;

    @Column
    private String buttonLink;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private boolean isActive = true;
}
