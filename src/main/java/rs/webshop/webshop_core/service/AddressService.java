package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.address.AddressRequest;
import rs.webshop.webshop_core.dto.address.AddressResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Address;
import rs.webshop.webshop_core.model.User;
import rs.webshop.webshop_core.repository.AddressRepository;
import rs.webshop.webshop_core.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN') or #request.userId == authentication.principal.id")
    public AddressResponse create(AddressRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = Address.builder()
                .user(user)
                .street(request.getStreet())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .build();

        return toResponse(addressRepository.save(address));
    }

    public AddressResponse getById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return toResponse(address);
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public List<AddressResponse> getByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AddressResponse> getAll() {
        return addressRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @authUtil.isOwnerOfAddress(#id, authentication.principal.id)")
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        return toResponse(addressRepository.save(address));
    }

    @PreAuthorize("hasRole('ADMIN') or @authUtil.isOwnerOfAddress(#id, authentication.principal.id)")
    public void delete(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .userFullName(address.getUser().getFirstName() + " " + address.getUser().getLastName())
                .street(address.getStreet())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }
}