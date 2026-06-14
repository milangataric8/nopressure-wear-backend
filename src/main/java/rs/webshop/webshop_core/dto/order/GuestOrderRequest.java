package rs.webshop.webshop_core.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GuestOrderRequest {

    @NotBlank(message = "Full name is required")
    private String customerFullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String customerEmail;

    @NotBlank(message = "Phone is required")
    private String customerPhone;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    @NotEmpty(message = "Order must have items")
    @Valid
    private List<GuestOrderItem> items;

    private String couponCode;
    private String paymentMethod;

    @Getter
    @Setter
    public static class GuestOrderItem {
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer quantity;
    }
}