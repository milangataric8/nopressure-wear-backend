package rs.nopressure.wear.webshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressure.wear.webshop.model.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository  extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findByParentIsNull();

    List<Category> findByParentId(Long parentId);

    boolean existsByName(String name);
}
