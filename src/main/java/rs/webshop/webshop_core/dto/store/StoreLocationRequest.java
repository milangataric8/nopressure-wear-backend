package rs.webshop.webshop_core.dto.store;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreLocationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    private String phone;
    private String email;
    private String workingHours;
}