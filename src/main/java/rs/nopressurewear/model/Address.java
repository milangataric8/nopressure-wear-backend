package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country;

    /**
     * At most one per user (DB partial unique index {@code uk_address_one_main_per_user}).
     * Named {@code main}, not {@code isMain}: Lombok's getter and Jackson's property name
     * both come out as {@code main}, so the Java field and the JSON key agree.
     */
    @Column(name = "is_main", nullable = false)
    private boolean main;
}