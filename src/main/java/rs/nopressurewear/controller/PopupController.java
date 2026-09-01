package rs.nopressurewear.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.popup.PopupRequest;
import rs.nopressurewear.dto.popup.PopupResponse;
import rs.nopressurewear.service.PopupService;

import java.util.List;

import static java.util.Objects.nonNull;

@RestController
@RequestMapping("/api/popups")
@RequiredArgsConstructor
public class PopupController {

    private final PopupService popupService;

    @PostMapping
    public ResponseEntity<PopupResponse> create(@Valid @RequestBody PopupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(popupService.create(request));
    }

    @GetMapping("/active")
    public ResponseEntity<PopupResponse> getActive() {
        PopupResponse popup = popupService.getActive();
        return nonNull(popup)
                ? ResponseEntity.ok(popup)
                : ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PopupResponse>> getAll() {
        return ResponseEntity.ok(popupService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PopupResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody PopupRequest request) {
        return ResponseEntity.ok(popupService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PopupResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(popupService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        popupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}