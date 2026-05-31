package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.StoreLocation;

import java.util.List;

@Repository
public interface StoreLocationRepository extends JpaRepository<StoreLocation, Long> {
    List<StoreLocation> findByActiveTrueOrderByNameAsc();
}