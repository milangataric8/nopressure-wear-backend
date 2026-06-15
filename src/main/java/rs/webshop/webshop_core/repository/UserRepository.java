package rs.webshop.webshop_core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.constants.Role;
import rs.webshop.webshop_core.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByIsActiveTrue();

    Optional<User> findByResetToken(String resetToken);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query(value = """
        SELECT * FROM users u
        WHERE u.role = :role
        AND (:search IS NULL
                    OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR u.is_active = :active)
        ORDER BY u.first_name ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM users u
        WHERE u.role = :role
        AND (:search IS NULL
                    OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR u.is_active = :active)
        """,
            nativeQuery = true)
    Page<User> findByFilters(
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("role") String role,
            Pageable pageable);

    @Query(
    value = """
        SELECT COUNT(*)
         FROM users
        WHERE role = 'CUSTOMER'
        """,
    nativeQuery = true)
    Long countCustomers();

    @Query(
    value = """
        SELECT COUNT(*)
         FROM users
        WHERE role = 'CUSTOMER'
          AND created_at >= :since
        """,
    nativeQuery = true)
    Long countCustomersSince(@Param("since") LocalDateTime since);

    @Query(
    value = """
        SELECT * FROM users
        WHERE role = :role
          AND is_active = true
        """,
    nativeQuery = true)
    List<User> findByRoleAndIsActiveTrue(@Param("role") String role);
}