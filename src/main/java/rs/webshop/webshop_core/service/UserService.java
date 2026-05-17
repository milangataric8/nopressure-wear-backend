package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.constants.Role;
import rs.webshop.webshop_core.dto.user.UserRequest;
import rs.webshop.webshop_core.dto.user.UserResponse;
import rs.webshop.webshop_core.dto.user.UserUpdateRequest;
import rs.webshop.webshop_core.exception.DuplicateResourceException;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.User;
import rs.webshop.webshop_core.repository.UserRepository;
import rs.webshop.webshop_core.security.AuthUtil;

import java.util.List;

import static java.util.Objects.nonNull;
import static rs.webshop.webshop_core.constants.Role.CUSTOMER;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;

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

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @authUtil.getCurrentUserId().equals(#id)")
    public UserResponse getById(Long id) {
        System.out.println("getCurrentUserId: " + authUtil.getCurrentUserId());
        System.out.println("requested id: " + id);
        System.out.println("equals: " + authUtil.getCurrentUserId().equals(id));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
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

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @authUtil.getCurrentUserId().equals(#id)")
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        setUserData(request, user);

        return toResponse(userRepository.save(user));
    }

    public void setUserData(UserUpdateRequest request, User user) {
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<UserResponse> getAllByRole(Pageable pageable) {
        return userRepository.findByRole(CUSTOMER, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<UserResponse> search(String search, Boolean active, Pageable pageable) {
        String searchParam = (nonNull(search) && !search.isBlank()) ? search : null;

        return userRepository.findByFilters(searchParam, active, CUSTOMER.name(), pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public UserResponse activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
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