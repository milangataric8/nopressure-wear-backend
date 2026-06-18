package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.user.UserRequest;
import rs.nopressurewear.dto.user.UserResponse;
import rs.nopressurewear.dto.user.UserUpdateRequest;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;

import static rs.nopressurewear.constants.Role.EMPLOYEE;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with this email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(EMPLOYEE)
                .isActive(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (user.getRole() != EMPLOYEE) {
            throw new RuntimeException("User is not an employee");
        }

        userService.setUserData(request, user);

        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findByRole(EMPLOYEE, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<UserResponse> search(String query, Boolean active, Pageable pageable) {
        return userRepository.findByFilters(query, active, EMPLOYEE.name(), pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (user.getRole() != EMPLOYEE) {
            throw new RuntimeException("User is not an employee");
        }

        userRepository.delete(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}