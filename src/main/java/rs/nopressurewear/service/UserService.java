package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.dto.user.UserRequest;
import rs.nopressurewear.dto.user.UserResponse;
import rs.nopressurewear.dto.user.UserUpdateRequest;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.AddressRepository;
import rs.nopressurewear.repository.CartRepository;
import rs.nopressurewear.repository.OrderRepository;
import rs.nopressurewear.repository.UserRepository;
import rs.nopressurewear.security.AuthUtil;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static rs.nopressurewear.constants.Role.CUSTOMER;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("validation.emailTaken", "email");
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
            throw new DuplicateResourceException("validation.emailTaken", "email");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        if (nonNull(request.getPassword()) && !request.getPassword().isBlank()) {
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

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void anonymizeCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("Only customer accounts can be anonymized");
        }

        String anonEmail = "deleted-" + user.getId() + "@anonymized.local";
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setEmail(anonEmail);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setEmailVerified(false);
        userRepository.save(user);

        addressRepository.findByUserId(id).forEach(addr -> {
            addr.setStreet("[removed]");
            addr.setCity("[removed]");
            addr.setPostalCode("[removed]");
            addressRepository.save(addr);
        });

        orderRepository.findByUserId(id, Pageable.unpaged()).getContent().forEach(order -> {
            order.setCustomerFullName("Deleted User");
            order.setCustomerEmail(anonEmail);
            order.setCustomerPhone(null);
            orderRepository.save(order);
        });

        cartRepository.findByUserId(id).ifPresent(cartRepository::delete);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void unlockAccount(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);
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