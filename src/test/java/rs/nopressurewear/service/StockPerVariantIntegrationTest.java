package rs.nopressurewear.service;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Gender;
import rs.nopressurewear.constants.ProductSize;
import rs.nopressurewear.dto.product.ProductResponse;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.ProductVariant;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.ProductVariantRepository;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class StockPerVariantIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private ProductService productService;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Product product(String name, String sku, boolean active) {
        return productRepository.save(Product.builder()
                .name(name).sku(sku).price(new BigDecimal("10.00"))
                .isActive(active).gender(Gender.UNISEX).build());
    }

    private void variant(Product p, ProductSize size, int stock) {
        productVariantRepository.save(ProductVariant.builder()
                .product(p).size(size).stockQuantity(stock).build());
    }

    // ---- findLowStockVariants: one row per variant ----

    @Test
    void findLowStockVariants_returnsOneRowPerVariantBelowThreshold() {
        Product a = product("Alpha", "A", true);
        variant(a, ProductSize.S, 50);   // above
        variant(a, ProductSize.M, 0);    // below
        Product b = product("Beta", "B", true);
        variant(b, ProductSize.S, 2);    // below
        variant(b, ProductSize.L, 3);    // below
        Product wellStocked = product("Gamma", "G", true);
        variant(wellStocked, ProductSize.M, 99);
        Product inactive = product("Delta-inactive", "D", false);
        variant(inactive, ProductSize.S, 0);

        List<Map<String, Object>> rows = productRepository.findLowStockVariants(5);

        assertThat(rows).hasSize(3);
        assertThat(rows).allSatisfy(r -> assertThat(((Number) r.get("stockQuantity")).intValue()).isLessThanOrEqualTo(5));
        // Alpha appears once, for M only
        assertThat(rows.stream().filter(r -> "Alpha".equals(r.get("name"))).toList())
                .singleElement()
                .satisfies(r -> assertThat(r.get("size")).isEqualTo("M"));
        // inactive excluded, well-stocked excluded
        assertThat(rows).noneSatisfy(r -> assertThat(r.get("name")).isIn("Delta-inactive", "Gamma"));
        // ordered by stock asc
        assertThat(((Number) rows.get(0).get("stockQuantity")).intValue()).isZero();
    }

    // ---- product response stockQuantity is the computed sum ----

    @Test
    void productResponse_stockQuantity_isSumOfVariants() {
        Product p = product("Summed", "S1", true);
        variant(p, ProductSize.S, 4);
        variant(p, ProductSize.M, 6);
        variant(p, ProductSize.L, 0);

        ProductResponse response = productService.getById(p.getId());

        assertThat(response.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void productResponse_stockQuantity_isZeroWhenNoVariants() {
        Product p = product("NoVariants", "NV", true);

        ProductResponse response = productService.getById(p.getId());

        assertThat(response.getStockQuantity()).isZero();
    }

    // ---- top sellers ordering: NULL sales must not rank first ----

    @Test
    void topSellingProducts_rankSoldProductsAboveNeverSold() {
        Product sold = product("Sold", "SOLD", true);
        sold.setSalesCount(5);
        productRepository.save(sold);
        Product neverSold = product("NeverSold", "NEVER", true);
        neverSold.setSalesCount(null);
        productRepository.save(neverSold);

        List<Map<String, Object>> top = productRepository.findTopSellingProducts(10);

        int soldIdx = indexOfName(top, "Sold");
        int neverIdx = indexOfName(top, "NeverSold");
        assertThat(soldIdx).isGreaterThanOrEqualTo(0);
        assertThat(neverIdx).isGreaterThanOrEqualTo(0);
        assertThat(soldIdx).isLessThan(neverIdx);
    }

    private static int indexOfName(List<Map<String, Object>> rows, String name) {
        for (int i = 0; i < rows.size(); i++) {
            if (name.equals(rows.get(i).get("name"))) return i;
        }
        return -1;
    }

    // ---- listing path does not add a per-product stock query ----

    @Test
    void listing_doesNotIssueAPerProductQueryForTheStockAggregate() {
        for (int i = 0; i < 6; i++) {
            Product p = product("List" + i, "L" + i, true);
            variant(p, ProductSize.S, 3);
            variant(p, ProductSize.M, 3);
        }
        productRepository.flush();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        stats.clear();
        Page<ProductResponse> three = productService.getActiveFiltered(
                null, null, null, null, null, null, null, null, null, PageRequest.of(0, 3));
        long q3 = stats.getPrepareStatementCount();

        stats.clear();
        Page<ProductResponse> six = productService.getActiveFiltered(
                null, null, null, null, null, null, null, null, null, PageRequest.of(0, 6));
        long q6 = stats.getPrepareStatementCount();

        assertThat(three.getContent()).allSatisfy(r -> assertThat(r.getStockQuantity()).isEqualTo(6));
        // per-product query count must be the pre-existing set (images, variants, colorVariants) — no 4th for stock
        long perProduct = (q6 - q3) / 3;
        assertThat(perProduct).isLessThanOrEqualTo(3);
    }
}
