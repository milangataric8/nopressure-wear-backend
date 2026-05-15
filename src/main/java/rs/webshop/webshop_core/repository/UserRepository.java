package rs.webshop.webshop_core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.constants.Role;
import rs.webshop.webshop_core.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByIsActiveTrue();

    Optional<User> findByResetToken(String resetToken);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByFirstNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name,
                                                                              String email,
                                                                              Pageable pageable);
}