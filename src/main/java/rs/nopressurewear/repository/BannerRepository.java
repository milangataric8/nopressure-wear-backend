package rs.nopressurewear.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.Banner;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();

    @Query(value = """
        SELECT * FROM banner b
        WHERE (:search IS NULL
                    OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR b.is_active = :active)
        ORDER BY b.title ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM banner b
        WHERE (:search IS NULL
                    OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
        AND (:active IS NULL OR b.is_active = :active)
        """,
            nativeQuery = true)
    Page<Banner> findByFilters(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}