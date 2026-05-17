package rs.webshop.webshop_core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.Category;
import rs.webshop.webshop_core.model.Product;

import java.util.List;

@Repository
public interface CategoryRepository  extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNull();

    List<Category> findByIsActiveTrueOrderByNameAsc();

    List<Category> findAllByParentId(Long parentCategoryId);

    boolean existsByName(String name);

    @Query(value = """
        SELECT * FROM category c
        WHERE (:search IS NULL
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR c.is_active = :active)
        ORDER BY c.name ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM category c
        WHERE (:search IS NULL
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR c.is_active = :active)
        """,
            nativeQuery = true)
    Page<Category> findByFilters(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}
