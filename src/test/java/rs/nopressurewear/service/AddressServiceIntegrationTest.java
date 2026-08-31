package rs.nopressurewear.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Role;
import rs.nopressurewear.dto.address.AddressRequest;
import rs.nopressurewear.dto.address.AddressResponse;
import rs.nopressurewear.model.Address;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.AddressRepository;
import rs.nopressurewear.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class AddressServiceIntegrationTest {

    @Autowired private AddressService addressService;
    @Autowired private AddressRepository addressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(String email) {
        return userRepository.save(User.builder()
                .firstName("Test").lastName("User").email(email)
                .password("x").role(Role.CUSTOMER).build());
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private AddressRequest request(Long userId, String street, boolean main) {
        AddressRequest r = new AddressRequest();
        r.setUserId(userId);
        r.setStreet(street);
        r.setCity("Novi Sad");
        r.setPostalCode("21000");
        r.setCountry("Serbia");
        r.setMain(main);
        return r;
    }

    private Address persistedAddress(User u, String street, boolean main) {
        return addressRepository.save(Address.builder()
                .user(u).street(street).city("Novi Sad").postalCode("21000").country("Serbia").main(main)
                .build());
    }

    @Test
    void create_withMainTrue_clearsMainOnOtherAddresses() {
        User u = user("a1@test.com");
        authenticateAs(u);
        Address existing = persistedAddress(u, "Old 1", true);

        AddressResponse created = addressService.create(request(u.getId(), "New 2", true));

        assertThat(created.isMain()).isTrue();
        assertThat(addressRepository.findById(existing.getId())).get()
                .satisfies(a -> assertThat(a.isMain()).isFalse());
        assertThat(addressRepository.findByUserId(u.getId())).filteredOn(Address::isMain).hasSize(1);
    }

    @Test
    void create_firstAddress_isMain_evenWhenMainFalseSent() {
        User u = user("a2@test.com");
        authenticateAs(u);

        AddressResponse created = addressService.create(request(u.getId(), "First", false));

        assertThat(created.isMain()).isTrue();
    }

    @Test
    void create_secondAddress_isNotMainByDefault() {
        User u = user("a3@test.com");
        authenticateAs(u);
        addressService.create(request(u.getId(), "First", false));   // becomes main (first)

        AddressResponse second = addressService.create(request(u.getId(), "Second", false));

        assertThat(second.isMain()).isFalse();
    }

    @Test
    void database_rejectsTwoMainAddressesForSameUser() {
        // CI generates the schema from entities (Flyway disabled), and JPA @Index cannot
        // express a partial index — so apply the exact DDL from V59 here and prove it bites.
        entityManager.createNativeQuery(
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_address_one_main_per_user "
                        + "ON address (user_id) WHERE is_main = true").executeUpdate();

        User u = user("a4@test.com");
        persistedAddress(u, "Main 1", true);

        assertThatThrownBy(() -> addressRepository.saveAndFlush(Address.builder()
                .user(u).street("Main 2").city("Novi Sad").postalCode("21000").country("Serbia").main(true)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void create_forAnotherUser_isRejected() {
        User owner = user("owner@test.com");
        User attacker = user("attacker@test.com");
        authenticateAs(attacker);

        assertThatThrownBy(() -> addressService.create(request(owner.getId(), "Hijack 1", false)))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(addressRepository.findByUserId(owner.getId())).isEmpty();
    }

    @Test
    void getByUser_returnsMainAddressFirst() {
        User u = user("a5@test.com");
        authenticateAs(u);
        persistedAddress(u, "Plain A", false);
        Address main = persistedAddress(u, "Main", true);
        persistedAddress(u, "Plain B", false);

        List<AddressResponse> list = addressService.getByUser(u.getId());

        assertThat(list.get(0).getId()).isEqualTo(main.getId());
        assertThat(list.get(0).isMain()).isTrue();
    }

    @Test
    void update_settingMain_clearsPreviousMain_andKeepsFieldEdits() {
        User u = user("a7@test.com");
        authenticateAs(u);
        Address wasMain = persistedAddress(u, "First", true);
        Address other = persistedAddress(u, "Second", false);

        AddressResponse updated = addressService.update(other.getId(), request(u.getId(), "Second edited", true));

        assertThat(updated.isMain()).isTrue();
        assertThat(updated.getStreet()).isEqualTo("Second edited");
        assertThat(addressRepository.findById(wasMain.getId())).get()
                .satisfies(a -> assertThat(a.isMain()).isFalse());
        assertThat(addressRepository.findByUserId(u.getId())).filteredOn(Address::isMain).hasSize(1);
    }

    @Test
    void delete_mainAddress_promotesMostRecentRemaining() {
        User u = user("a6@test.com");
        authenticateAs(u);
        Address main = persistedAddress(u, "Main", true);
        Address older = persistedAddress(u, "Older", false);
        Address newer = persistedAddress(u, "Newer", false);

        addressService.delete(main.getId());

        assertThat(addressRepository.findById(newer.getId())).get()
                .satisfies(a -> assertThat(a.isMain()).isTrue());
        assertThat(addressRepository.findById(older.getId())).get()
                .satisfies(a -> assertThat(a.isMain()).isFalse());
    }
}
