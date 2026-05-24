package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.product.ProductAttributeRequest;
import rs.webshop.webshop_core.dto.product.ProductAttributeResponse;
import rs.webshop.webshop_core.service.ProductAttributeService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductAttributeController {

    private final ProductAttributeService productAttributeService;

    @PostMapping("/{id}/attributes")
    public ResponseEntity<ProductAttributeResponse> addAttribute(
            @PathVariable Long id,
            @Valid @RequestBody ProductAttributeRequest request) {
        return ResponseEntity.status(CREATED)
                .body(productAttributeService.addAttribute(id, request));
    }

    @DeleteMapping("/attributes/{attributeId}")
    public ResponseEntity<Void> deleteAttribute(@PathVariable Long attributeId) {
        productAttributeService.deleteAttribute(attributeId);
        return ResponseEntity.noContent().build();
    }
}
