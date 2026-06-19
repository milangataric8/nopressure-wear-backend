package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.product.ProductColorVariantResponse;
import rs.nopressurewear.service.ColorVariantService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductColorVariantController {

    private final ColorVariantService colorVariantService;

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductColorVariantResponse>> getVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(colorVariantService.getVariants(productId));
    }

    @PostMapping("/{productId}/variants/{variantProductId}")
    public ResponseEntity<List<ProductColorVariantResponse>> linkVariant(
            @PathVariable Long productId,
            @PathVariable Long variantProductId) {
        return ResponseEntity.ok(colorVariantService.linkVariant(productId, variantProductId));
    }

    @DeleteMapping("/{productId}/variants/{variantProductId}")
    public ResponseEntity<List<ProductColorVariantResponse>> unlinkVariant(
            @PathVariable Long productId,
            @PathVariable Long variantProductId) {
        return ResponseEntity.ok(colorVariantService.unlinkVariant(productId, variantProductId));
    }
}