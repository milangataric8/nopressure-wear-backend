package rs.webshop.webshop_core.dto.store;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StoreLocationResponse {
    private Long id;
    private String name;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
    private String workingHours;
    private Boolean active;
}