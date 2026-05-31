package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.webshop.webshop_core.dto.store.*;
import rs.webshop.webshop_core.exception.DuplicateResourceException;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Product;
import rs.webshop.webshop_core.model.ProductStore;
import rs.webshop.webshop_core.model.StoreLocation;
import rs.webshop.webshop_core.repository.ProductRepository;
import rs.webshop.webshop_core.repository.ProductStoreRepository;
import rs.webshop.webshop_core.repository.StoreLocationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreLocationService {

    private final StoreLocationRepository storeLocationRepository;
    private final ProductStoreRepository productStoreRepository;
    private final ProductRepository productRepository;

    public List<StoreLocationResponse> getActive() {
        return storeLocationRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<StoreLocationResponse> getAll() {
        return storeLocationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public StoreLocationResponse create(StoreLocationRequest request) {
        StoreLocation store = StoreLocation.builder()
                .name(request.getName())
                .street(request.getStreet())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .workingHours(request.getWorkingHours())
                .active(true)
                .build();
        return toResponse(storeLocationRepository.save(store));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public StoreLocationResponse update(Long id, StoreLocationRequest request) {
        StoreLocation store = storeLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store location not found"));

        store.setName(request.getName());
        store.setStreet(request.getStreet());
        store.setCity(request.getCity());
        store.setPostalCode(request.getPostalCode());
        store.setCountry(request.getCountry());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setWorkingHours(request.getWorkingHours());

        return toResponse(storeLocationRepository.save(store));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public StoreLocationResponse toggleActive(Long id) {
        StoreLocation store = storeLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store location not found"));
        store.setActive(!store.isActive());
        return toResponse(storeLocationRepository.save(store));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        StoreLocation store = storeLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store location not found"));
        storeLocationRepository.delete(store);
    }

    // Product-Store linking
    public List<ProductStoreResponse> getStoresForProduct(Long productId) {
        return productStoreRepository.findByProductIdAndInStockTrue(productId)
                .stream()
                .map(this::toProductStoreResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<ProductStoreResponse> getAllStoresForProduct(Long productId) {
        return productStoreRepository.findByProductId(productId)
                .stream()
                .map(this::toProductStoreResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ProductStoreResponse addProductToStore(Long productId, ProductStoreRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        StoreLocation store = storeLocationRepository.findById(request.getStoreLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Store location not found"));

        if (productStoreRepository.existsByProductIdAndStoreLocationId(productId, request.getStoreLocationId())) {
            throw new DuplicateResourceException("Product already linked to this store");
        }

        ProductStore productStore = ProductStore.builder()
                .product(product)
                .storeLocation(store)
                .inStock(request.getInStock() != null ? request.getInStock() : true)
                .build();

        return toProductStoreResponse(productStoreRepository.save(productStore));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ProductStoreResponse toggleProductStoreStock(Long id) {
        ProductStore productStore = productStoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product-store link not found"));
        productStore.setInStock(!productStore.isInStock());
        return toProductStoreResponse(productStoreRepository.save(productStore));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional
    public void removeProductFromStore(Long productId, Long storeLocationId) {
        productStoreRepository.deleteByProductIdAndStoreLocationId(productId, storeLocationId);
    }

    private StoreLocationResponse toResponse(StoreLocation store) {
        return StoreLocationResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .street(store.getStreet())
                .city(store.getCity())
                .postalCode(store.getPostalCode())
                .country(store.getCountry())
                .phone(store.getPhone())
                .email(store.getEmail())
                .workingHours(store.getWorkingHours())
                .active(store.isActive())
                .build();
    }

    private ProductStoreResponse toProductStoreResponse(ProductStore ps) {
        return ProductStoreResponse.builder()
                .id(ps.getId())
                .storeLocationId(ps.getStoreLocation().getId())
                .storeName(ps.getStoreLocation().getName())
                .storeStreet(ps.getStoreLocation().getStreet())
                .storeCity(ps.getStoreLocation().getCity())
                .storeCountry(ps.getStoreLocation().getCountry())
                .storePhone(ps.getStoreLocation().getPhone())
                .storeWorkingHours(ps.getStoreLocation().getWorkingHours())
                .inStock(ps.isInStock())
                .build();
    }
}