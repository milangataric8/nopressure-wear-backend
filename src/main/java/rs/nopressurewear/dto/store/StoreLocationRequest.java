package rs.nopressurewear.dto.store;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreLocationRequest {

    @NotBlank(message = "validation.name.required")
    private String name;

    @NotBlank(message = "validation.street.required")
    private String street;

    @NotBlank(message = "validation.city.required")
    private String city;

    private String postalCode;

    @NotBlank(message = "validation.country.required")
    private String country;

    private String phone;
    private String email;
    private String workingHours;
}