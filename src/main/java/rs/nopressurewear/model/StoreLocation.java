package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column
    private String postalCode;

    @Column(nullable = false)
    private String country;

    @Column
    private String phone;

    @Column
    private String email;

    @Column
    private String workingHours;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}