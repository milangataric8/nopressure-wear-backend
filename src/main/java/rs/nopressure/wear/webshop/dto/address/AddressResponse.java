package rs.nopressure.wear.webshop.dto.address;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String street;
    private String city;
    private String postalCode;
    private String country;
}