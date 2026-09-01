package rs.nopressurewear.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.constants.ProductSize;
import rs.nopressurewear.dto.product.*;
import rs.nopressurewear.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .status(CREATED)
                .body(productService.create(request));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ProductImageResponse> addImage(
            @PathVariable Long id,
            @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity
                .status(CREATED)
                .body(productService.addImage(id, request));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        productService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String colorName,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) List<ProductSize> sizes,
            @PageableDefault(sort = "name") Pageable pageable) {

        if (haveFilters(categoryId, search, active, brand, colorName, material, gender, sizes)) {
            return ResponseEntity.ok(
                    productService.
                            filter(categoryId, search, active, brand, colorName, material, gender, sizes, pageable));
        }

        return ResponseEntity.ok(productService.getAll(pageable));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeatured(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(productService.getMostSold(limit));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ProductResponse>> getSimilar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(productService.getSimilarProducts(id, limit));
    }

    private static boolean haveFilters(Long categoryId,
                                       String search,
                                       Boolean active,
                                       String brand,
                                       String colorName,
                                       String material,
                                       String gender,
                                       List<ProductSize> sizes) {
        return (nonNull(search) && !search.isBlank())
                || nonNull(categoryId)
                || nonNull(active)
                || nonNull(brand)
                || nonNull(colorName)
                || nonNull(material)
                || (nonNull(sizes) && !sizes.isEmpty())
                || (nonNull(gender)
                && !gender.isBlank());
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponse>> getByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @PageableDefault(size = 8, sort = "price") Pageable pageable) {

        return ResponseEntity.ok(productService.getByPriceRange(minPrice, maxPrice, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<ProductResponse>> getActive(
            @PageableDefault(sort = "name") Pageable pageable) {

        return ResponseEntity.ok(productService.getActive(pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(sort = "name") Pageable pageable) {

        return ResponseEntity.ok(productService.getByCategory(categoryId, pageable));
    }

    @GetMapping("/by-categories")
    public ResponseEntity<Page<ProductResponse>> getByCategories(
            @RequestParam List<Long> categoryIds,
            @PageableDefault(size = 8) Pageable pageable) {

        return ResponseEntity.ok(productService.getByCategories(categoryIds, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String colorName,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) List<ProductSize> sizes,
            @PageableDefault(sort = "name") Pageable pageable) {

        return ResponseEntity.ok(productService.getActiveFiltered(
                categoryId, search, minPrice, maxPrice, brand, colorName, material, gender, sizes, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ProductResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleActive(id));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getAvailableFilters() {
        return ResponseEntity.ok(productService.getAvailableFilters());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}