package rs.nopressurewear.catalog.api.dto.product;

import java.util.List;

/** A presentation group; each option remains an independently purchasable product. */
public record CatalogModelResponse(Long id, List<ProductResponse> options) {}
