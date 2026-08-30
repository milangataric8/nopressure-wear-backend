package rs.nopressurewear.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Gender;
import rs.nopressurewear.constants.ProductSize;
import rs.nopressurewear.dto.product.ProductResponse;
import rs.nopressurewear.model.Category;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.ProductVariant;
import rs.nopressurewear.repository.CategoryRepository;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.ProductVariantRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@Transactional
class SizeFilterIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductService productService;
    @Autowired private MockMvc mockMvc;
    @Autowired private CacheManager cacheManager;

    private static final PageRequest PAGE = PageRequest.of(0, 50);

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @BeforeEach
    void clearFilterCache() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    private Product product(String name, String sku, String price, boolean active, Category category) {
        return productRepository.save(Product.builder()
                .name(name).sku(sku).price(new BigDecimal(price))
                .isActive(active).gender(Gender.UNISEX).category(category)
                .build());
    }

    private void variant(Product p, ProductSize size, int stock) {
        productVariantRepository.save(ProductVariant.builder()
                .product(p).size(size).stockQuantity(stock).build());
    }

    private List<Long> ids(Page<ProductResponse> page) {
        return page.getContent().stream().map(ProductResponse::getId).toList();
    }

    private Page<ProductResponse> filterBySizes(List<ProductSize> sizes) {
        return productService.getActiveFiltered(null, null, null, null, null, null, null, null, sizes, PAGE);
    }

    @Test
    void nullSizes_returnsAllActiveProducts_unchanged() {
        Product a = product("Alpha", "A", "10", true, null);
        variant(a, ProductSize.M, 5);
        Product b = product("Beta", "B", "20", true, null);
        variant(b, ProductSize.L, 5);
        product("Gamma-inactive", "G", "30", false, null);

        Page<ProductResponse> result = filterBySizes(null);

        assertThat(ids(result)).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void singleSize_returnsOnlyProductsWithThatSizeInStock() {
        Product hasM = product("HasM", "HM", "10", true, null);
        variant(hasM, ProductSize.M, 3);
        Product onlyL = product("OnlyL", "OL", "10", true, null);
        variant(onlyL, ProductSize.L, 3);
        Product mSoldOut = product("MSoldOut", "MS", "10", true, null);
        variant(mSoldOut, ProductSize.M, 0);

        Page<ProductResponse> result = filterBySizes(List.of(ProductSize.M));

        assertThat(ids(result)).containsExactly(hasM.getId());
    }

    @Test
    void multipleSizes_returnUnion_eachProductOnce() {
        Product mAndL = product("MandL", "ML", "10", true, null);
        variant(mAndL, ProductSize.M, 2);
        variant(mAndL, ProductSize.L, 2);
        Product justL = product("JustL", "JL", "10", true, null);
        variant(justL, ProductSize.L, 2);
        Product justS = product("JustS", "JS", "10", true, null);
        variant(justS, ProductSize.S, 2);

        Page<ProductResponse> result = filterBySizes(List.of(ProductSize.M, ProductSize.L));

        assertThat(ids(result)).containsExactlyInAnyOrder(mAndL.getId(), justL.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(ids(result)).doesNotHaveDuplicates();
    }

    @Test
    void sizeFilter_combinesWithCategoryAndPrice() {
        Category cat = categoryRepository.save(Category.builder().name("Shoes").isActive(true).build());
        Category other = categoryRepository.save(Category.builder().name("Hats").isActive(true).build());

        Product cheapInCat = product("CheapInCat", "CIC", "50", true, cat);
        variant(cheapInCat, ProductSize.M, 5);
        Product pricyInCat = product("PricyInCat", "PIC", "500", true, cat);
        variant(pricyInCat, ProductSize.M, 5);
        Product cheapOtherCat = product("CheapOther", "CO", "50", true, other);
        variant(cheapOtherCat, ProductSize.M, 5);

        Page<ProductResponse> result = productService.getActiveFiltered(
                cat.getId(), null, null, new BigDecimal("100"), null, null, null, null,
                List.of(ProductSize.M), PAGE);

        assertThat(ids(result)).containsExactly(cheapInCat.getId());
    }

    @Test
    void unknownSizeValue_returns400() throws Exception {
        mockMvc.perform(get("/api/products").param("sizes", "XXL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availableSizes_inEnumOrder_excludingNoStockAndInactive() {
        Product p1 = product("P1", "P1", "10", true, null);
        variant(p1, ProductSize.L, 4);
        variant(p1, ProductSize.S, 4);
        Product p2 = product("P2", "P2", "10", true, null);
        variant(p2, ProductSize.M, 4);
        variant(p2, ProductSize.XL, 0);          // no stock -> excluded
        Product inactive = product("P3", "P3", "10", false, null);
        variant(inactive, ProductSize.XL, 9);    // inactive product -> excluded

        @SuppressWarnings("unchecked")
        List<String> sizes = (List<String>) productService.getAvailableFilters().get("sizes");

        assertThat(sizes).containsExactly("S", "M", "L");
    }
}
