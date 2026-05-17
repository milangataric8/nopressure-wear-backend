package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.user.UserRequest;
import rs.webshop.webshop_core.dto.user.UserResponse;
import rs.webshop.webshop_core.dto.user.UserUpdateRequest;
import rs.webshop.webshop_core.service.UserService;

import java.util.List;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.CREATED;
import static rs.webshop.webshop_core.constants.Role.CUSTOMER;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(CREATED).body(userService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    @GetMapping("/customers")
    public ResponseEntity<Page<UserResponse>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        if ((nonNull(search) && !search.isBlank())
                || nonNull(active)) {
            return search(search, active, pageable);
        }

        return getAll(pageable);
    }

    private ResponseEntity<Page<UserResponse>> search(String search, Boolean active, Pageable pageable) {
        return ResponseEntity.ok(userService.search(search, active, pageable));
    }

    private ResponseEntity<Page<UserResponse>> getAll(
            @PageableDefault(sort = "firstName") Pageable pageable) {

        return ResponseEntity.ok(userService.getAllByRole(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}