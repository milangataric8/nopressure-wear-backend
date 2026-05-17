package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.constants.Role;
import rs.webshop.webshop_core.dto.user.UserRequest;
import rs.webshop.webshop_core.dto.user.UserResponse;
import rs.webshop.webshop_core.dto.user.UserUpdateRequest;
import rs.webshop.webshop_core.service.EmployeeService;
import rs.webshop.webshop_core.service.UserService;

import static java.util.Objects.nonNull;
import static rs.webshop.webshop_core.constants.Role.EMPLOYEE;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAll(
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
        return ResponseEntity.ok(employeeService.search(search, active, pageable));
    }

    private ResponseEntity<Page<UserResponse>> getAll(
            @PageableDefault(sort = "firstName") Pageable pageable) {

        return ResponseEntity.ok(employeeService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.activate(id));
    }
}