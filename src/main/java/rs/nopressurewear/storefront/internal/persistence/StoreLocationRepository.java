package rs.nopressurewear.storefront.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.storefront.internal.domain.StoreLocation;

import java.util.List;

@Repository
public interface StoreLocationRepository extends JpaRepository<StoreLocation, Long> {

    List<StoreLocation> findByActiveTrueOrderByNameAsc();
}