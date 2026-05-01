package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.user.UserRequest;
import rs.webshop.webshop_core.dto.user.UserResponse;
import rs.webshop.webshop_core.exception.DuplicateResourceException;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.User;
import rs.webshop.webshop_core.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with this email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserResponse> getActiveUsers() {
        return userRepository.findByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
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