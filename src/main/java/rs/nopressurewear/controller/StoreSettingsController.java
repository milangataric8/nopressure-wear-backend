package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.settings.StoreSettingsRequest;
import rs.nopressurewear.dto.settings.StoreSettingsResponse;
import rs.nopressurewear.service.StoreSettingsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    @GetMapping
    public ResponseEntity<List<StoreSettingsResponse>> getAll() {
        return ResponseEntity.ok(storeSettingsService.getAll());
    }

    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getAllAsMap() {
        return ResponseEntity.ok(storeSettingsService.getAllAsMap());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreSettingsResponse> update(
            @PathVariable Long id,
            @RequestBody StoreSettingsRequest request) {
        return ResponseEntity.ok(storeSettingsService.update(id, request));
    }
}