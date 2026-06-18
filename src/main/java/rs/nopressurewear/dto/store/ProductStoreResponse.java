package rs.nopressurewear.dto.store;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductStoreResponse {
    private Long id;
    private Long storeLocationId;
    private String storeName;
    private String storeStreet;
    private String storeCity;
    private String storeCountry;
    private String storePhone;
    private String storeWorkingHours;
    private Boolean inStock;
}