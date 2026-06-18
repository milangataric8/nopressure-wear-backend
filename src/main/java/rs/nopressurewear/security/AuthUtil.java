package rs.nopressurewear.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.AddressRepository;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final AddressRepository addressRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isNull(authentication) || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public boolean isAdmin() {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isEmployee() {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    public boolean isOwnerOfAddress(Long addressId, Long userId) {
        return addressRepository.findById(addressId)
                .map(address -> address.getUser().getId().equals(userId))
                .orElse(false);
    }
}