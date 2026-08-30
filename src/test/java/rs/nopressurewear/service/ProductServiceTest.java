package rs.nopressurewear.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.nopressurewear.dto.product.ProductRequest;
import rs.nopressurewear.dto.product.ProductResponse;
import rs.nopressurewear.dto.store.ProductStoreRequest;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Category;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.repository.CategoryRepository;
import rs.nopressurewear.repository.ProductColorVariantRepository;
import rs.nopressurewear.repository.ProductImageRepository;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.ProductVariantRepository;
import rs.nopressurewear.repository.StoreLocationRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductColorVariantRepository colorVariantRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private StoreLocationRepository storeLocationRepository;

    @Mock
    private ColorVariantService colorVariantService;

    @Mock
    private StoreLocationService storeLocationService;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequest request;
    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        product = Product.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .description("Latest Apple smartphone")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .sku("APPL-IPH15PRO")
                .isActive(true)
                .category(category)
                .build();

        request = new ProductRequest();
        request.setName("iPhone 15 Pro");
        request.setDescription("Latest Apple smartphone");
        request.setPrice(new BigDecimal("999.99"));
        request.setStockQuantity(50);
        request.setSku("APPL-IPH15PRO");
        request.setCategoryId(1L);

        lenient().when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());
        lenient().when(colorVariantRepository.findByProductId(anyLong())).thenReturn(List.of());
        lenient().when(productRepository.findBySkuContainingAndIdNot(anyString(), anyLong())).thenReturn(List.of());
        lenient().when(productVariantRepository.findByProductId(anyLong())).thenReturn(List.of());
        lenient().when(productVariantRepository.saveAll(any())).thenReturn(List.of());
    }

    @Test
    void create_ShouldReturnProductResponse_WhenValidRequest() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("999.99"));
        verify(productRepository, atLeastOnce()).save(any(Product.class));
    }

    @Test
    void create_ShouldNormalizeWhitespaceInDescription() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        request.setDescription("izaberes&nbsp;svedenu&nbsp;belu&nbsp;polo&nbsp;majicu, ostalo 10 kg");

        productService.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(captor.capture());
        // first save() receives the freshly built entity with the sanitized description
        String saved = captor.getAllValues().get(0).getDescription();
        assertThat(saved).doesNotContain("&nbsp;svedenu");
        assertThat(saved).contains("izaberes svedenu belu polo majicu");
        assertThat(saved).contains("10&nbsp;kg");
    }

    @Test
    void create_ShouldThrowNotFoundException_WhenCategoryNotFound() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(productRepository, never()).save(any());
    }

    // ---- nested relations on create ----

    private ProductStoreRequest storeReq(Long storeLocationId, boolean inStock) {
        ProductStoreRequest r = new ProductStoreRequest();
        r.setStoreLocationId(storeLocationId);
        r.setInStock(inStock);
        return r;
    }

    @Test
    void create_WithVariantsAndStores_PersistsAllRelationsInOneTransaction() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productRepository.existsById(2L)).thenReturn(true);
        when(storeLocationRepository.existsById(3L)).thenReturn(true);

        request.setColorVariantIds(List.of(2L));
        request.setStores(List.of(storeReq(3L, true)));

        productService.create(request);

        verify(colorVariantService).linkColorVariant(1L, 2L);
        ArgumentCaptor<ProductStoreRequest> storeCaptor = ArgumentCaptor.forClass(ProductStoreRequest.class);
        verify(storeLocationService).addProductToStore(eq(1L), storeCaptor.capture());
        assertThat(storeCaptor.getValue().getStoreLocationId()).isEqualTo(3L);
        verify(storeLocationService).getAllStoresForProduct(1L);
    }

    @Test
    void create_WithNullRelations_PersistsProductWithNoRelations() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        request.setColorVariantIds(null);
        request.setStores(null);

        ProductResponse response = productService.create(request);

        assertThat(response).isNotNull();
        verify(colorVariantService, never()).linkColorVariant(any(), any());
        verify(storeLocationService, never()).addProductToStore(any(), any());
    }

    @Test
    void create_WithEmptyLists_BehavesSameAsNull() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        request.setColorVariantIds(List.of());
        request.setStores(List.of());

        productService.create(request);

        verify(colorVariantService, never()).linkColorVariant(any(), any());
        verify(storeLocationService, never()).addProductToStore(any(), any());
    }

    @Test
    void update_WithNullRelations_LeavesExistingRelationsUntouched() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        request.setColorVariantIds(null);
        request.setStores(null);

        productService.update(1L, request);

        verifyNoInteractions(colorVariantService, storeLocationService);
    }

    @Test
    void create_WithInvalidVariantId_RollsBackWithNoProductSaved() {
        when(productRepository.existsById(999L)).thenReturn(false);
        request.setColorVariantIds(List.of(999L));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("999");

        verify(productRepository, never()).save(any());
        verifyNoInteractions(colorVariantService);
    }

    @Test
    void create_WithInvalidStoreId_RollsBackWithNoProductSaved() {
        when(storeLocationRepository.existsById(999L)).thenReturn(false);
        request.setStores(List.of(storeReq(999L, true)));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("999");

        verify(productRepository, never()).save(any());
        verifyNoInteractions(storeLocationService);
    }

    @Test
    void create_WithDuplicateVariantIds_ThrowsFieldValidation() {
        request.setColorVariantIds(List.of(2L, 2L));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("Duplicate");

        verify(productRepository, never()).save(any());
    }

    @Test
    void create_WithDuplicateStoreIds_ThrowsFieldValidation() {
        request.setStores(List.of(storeReq(3L, true), storeReq(3L, false)));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("Duplicate");

        verify(productRepository, never()).save(any());
    }

    @Test
    void getById_ShouldReturnProduct_WhenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getSku()).isEqualTo("APPL-IPH15PRO");
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotExists() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_ShouldReturnPagedProducts() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> responses = productService.getAll(PageRequest.of(0, 10));

        assertThat(responses).isNotNull();
        assertThat(responses.getContent()).hasSize(1);
    }

    @Test
    void toggleActive_ShouldSetInactive_WhenProductIsActive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.toggleActive(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void delete_ShouldDelete_WhenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository, times(1)).delete(product);
    }
}
