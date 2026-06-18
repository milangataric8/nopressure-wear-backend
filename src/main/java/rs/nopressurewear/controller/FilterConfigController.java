package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.filter.FilterConfigRequest;
import rs.nopressurewear.dto.filter.FilterConfigResponse;
import rs.nopressurewear.service.FilterConfigService;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
public class FilterConfigController {

    private final FilterConfigService filterConfigService;

    @PostMapping
    public ResponseEntity<FilterConfigResponse> create(@RequestBody Map<String, String> request) {
        return ResponseEntity.status(CREATED)
                .body(filterConfigService.create(
                        request.get("fieldName"),
                        request.get("displayName"),
                        request.get("filterType")
                ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        filterConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/visible")
    public ResponseEntity<List<FilterConfigResponse>> getVisible() {
        return ResponseEntity.ok(filterConfigService.getVisible());
    }

    @GetMapping
    public ResponseEntity<List<FilterConfigResponse>> getAll() {
        return ResponseEntity.ok(filterConfigService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FilterConfigResponse> update(
            @PathVariable Long id,
            @RequestBody FilterConfigRequest request) {
        return ResponseEntity.ok(filterConfigService.update(id, request));
    }
}