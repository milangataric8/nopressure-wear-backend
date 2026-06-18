package rs.nopressurewear.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.store.*;
import rs.nopressurewear.service.StoreLocationService;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreLocationController {

    private final StoreLocationService storeLocationService;

    @GetMapping("/active")
    public ResponseEntity<List<StoreLocationResponse>> getActive() {
        return ResponseEntity.ok(storeLocationService.getActive());
    }

    @GetMapping
    public ResponseEntity<List<StoreLocationResponse>> getAll() {
        return ResponseEntity.ok(storeLocationService.getAll());
    }

    @PostMapping
    public ResponseEntity<StoreLocationResponse> create(@Valid @RequestBody StoreLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeLocationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreLocationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody StoreLocationRequest request) {
        return ResponseEntity.ok(storeLocationService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<StoreLocationResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(storeLocationService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storeLocationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Product-Store endpoints
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductStoreResponse>> getStoresForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(storeLocationService.getStoresForProduct(productId));
    }

    @GetMapping("/product/{productId}/all")
    public ResponseEntity<List<ProductStoreResponse>> getAllStoresForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(storeLocationService.getAllStoresForProduct(productId));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<ProductStoreResponse> addProductToStore(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeLocationService.addProductToStore(productId, request));
    }

    @PatchMapping("/product-store/{id}/toggle")
    public ResponseEntity<ProductStoreResponse> toggleStock(@PathVariable Long id) {
        return ResponseEntity.ok(storeLocationService.toggleProductStoreStock(id));
    }

    @DeleteMapping("/product/{productId}/store/{storeLocationId}")
    public ResponseEntity<Void> removeProductFromStore(
            @PathVariable Long productId,
            @PathVariable Long storeLocationId) {
        storeLocationService.removeProductFromStore(productId, storeLocationId);
        return ResponseEntity.noContent().build();
    }
}