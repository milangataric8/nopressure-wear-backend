package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.dto.address.AddressRequest;
import rs.nopressurewear.dto.address.AddressResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Address;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.AddressRepository;
import rs.nopressurewear.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or #request.userId == authentication.principal.id")
    public AddressResponse create(AddressRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isFirst = addressRepository.countByUserId(user.getId()) == 0;
        boolean makeMain = request.isMain() || isFirst;

        if (makeMain) {
            addressRepository.clearMainForUser(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .street(request.getStreet())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .main(makeMain)
                .build();

        return toResponse(addressRepository.save(address));
    }

    public AddressResponse getById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return toResponse(address);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or #userId == authentication.principal.id")
    public List<AddressResponse> getByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return addressRepository.findByUserIdOrderByMainDescIdAsc(userId)
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

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @authUtil.isOwnerOfAddress(#id, authentication.principal.id)")
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        if (request.isMain() && !address.isMain()) {
            addressRepository.findByUserIdAndMainTrue(address.getUser().getId())
                    .ifPresent(current -> current.setMain(false));
            addressRepository.flush();
            address.setMain(true);
        }

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @authUtil.isOwnerOfAddress(#id, authentication.principal.id)")
    public void delete(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        boolean wasMain = address.isMain();
        Long userId = address.getUser().getId();

        addressRepository.delete(address);

        // Deleting the main address must not leave the user without one: promote the most
        // recently created remaining address. Flush the delete first so the DB no longer
        // holds a main row when the promotion update runs against the partial unique index.
        if (wasMain) {
            addressRepository.flush();
            addressRepository.findByUserIdOrderByMainDescIdAsc(userId).stream()
                    .max(Comparator.comparing(Address::getId))
                    .ifPresent(promoted -> promoted.setMain(true));
        }
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
                .main(address.isMain())
                .build();
    }
}
