package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.address.AddressRequest;
import rs.webshop.webshop_core.dto.address.AddressResponse;
import rs.webshop.webshop_core.dto.category.CategoryResponse;
import rs.webshop.webshop_core.service.AddressService;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(CREATED).body(addressService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AddressResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getByUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAll() {
        return ResponseEntity.ok(addressService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}