package rs.nopressurewear.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.Coupon;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    @Query(value = """
        SELECT * FROM coupon c
        WHERE (:search IS NULL
                    OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR c.is_active = :active)
        ORDER BY c.code ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM coupon c
        WHERE (:search IS NULL
                    OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
        AND (:active IS NULL OR c.is_active = :active)
        """,
            nativeQuery = true)
    Page<Coupon> findByFilters(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}