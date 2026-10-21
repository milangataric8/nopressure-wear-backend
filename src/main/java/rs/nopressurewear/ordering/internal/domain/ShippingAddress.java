package rs.nopressurewear.ordering.internal.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress {

    private String street;
    private String city;
    private String postalCode;
    private String country;
}