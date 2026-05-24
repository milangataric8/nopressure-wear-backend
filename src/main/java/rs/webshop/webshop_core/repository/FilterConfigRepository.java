package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.FilterConfig;

import java.util.List;

@Repository
public interface FilterConfigRepository extends JpaRepository<FilterConfig, Long> {
    List<FilterConfig> findByVisibleTrueOrderByDisplayOrderAsc();
    List<FilterConfig> findAllByOrderByDisplayOrderAsc();
}