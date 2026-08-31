package rs.nopressurewear.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.Gender;
import rs.nopressurewear.constants.OrderStatus;
import rs.nopressurewear.model.Category;
import rs.nopressurewear.model.Order;
import rs.nopressurewear.model.OrderItem;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.repository.CategoryRepository;
import rs.nopressurewear.repository.OrderRepository;
import rs.nopressurewear.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the hierarchy fields on {@code OrderRepository.getRevenueByCategory()}:
 * category id + parent id/name per row, top-level rows with null parent fields,
 * per-{@code category_id} revenue attribution, and revenue-descending order.
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class RevenueByCategoryIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;

    private Category category(String name, Category parent) {
        return categoryRepository.save(Category.builder().name(name).parent(parent).isActive(true).build());
    }

    private Product product(String name, String price, Category category) {
        return productRepository.save(Product.builder()
                .name(name).sku(name).price(new BigDecimal(price))
                .isActive(true).gender(Gender.UNISEX).category(category)
                .build());
    }

    private void order(String code, OrderItem... items) {
        Order o = Order.builder()
                .status(OrderStatus.DELIVERED)
                .totalAmount(BigDecimal.ZERO)
                .orderCode(code)
                .build();
        for (OrderItem it : items) {
            it.setOrder(o);
            o.getOrderItems().add(it);
        }
        orderRepository.save(o);
    }

    private OrderItem item(Product p, int qty, String price) {
        return OrderItem.builder().product(p).quantity(qty).priceAtPurchase(new BigDecimal(price)).build();
    }

    private static Map<String, Object> rowFor(List<Map<String, Object>> rows, String categoryName) {
        return rows.stream()
                .filter(r -> categoryName.equals(r.get("categoryname")))
                .findFirst().orElseThrow(() -> new AssertionError("no row for " + categoryName));
    }

    private static long revenue(Map<String, Object> row) {
        return new BigDecimal(row.get("revenue").toString()).longValueExact();
    }

    @Test
    void projection_carriesCategoryIdAndParent_topLevelRowsHaveNullParent() {
        Category clothing = category("Clothing", null);
        Category shirts = category("Shirts", clothing);
        Category shoes = category("Shoes", null);

        Product shirtProduct = product("ShirtA", "10", shirts);
        Product clothingDirect = product("GiftCard", "100", clothing);   // attached straight to the parent
        Product shoeProduct = product("SneakerA", "5", shoes);

        order("RBC-1",
                item(shirtProduct, 2, "10"),      // Shirts   -> 20
                item(clothingDirect, 1, "100"),   // Clothing -> 100
                item(shoeProduct, 3, "5"));       // Shoes    -> 15

        List<Map<String, Object>> rows = orderRepository.getRevenueByCategory();

        // every row exposes the identity + hierarchy fields
        assertThat(rows).allSatisfy(r -> assertThat(r).containsKeys("categoryid", "categoryname", "parentid", "parentname", "revenue"));

        Map<String, Object> shirtsRow = rowFor(rows, "Shirts");
        assertThat(((Number) shirtsRow.get("categoryid")).longValue()).isEqualTo(shirts.getId());
        assertThat(((Number) shirtsRow.get("parentid")).longValue()).isEqualTo(clothing.getId());
        assertThat(shirtsRow.get("parentname")).isEqualTo("Clothing");
        assertThat(revenue(shirtsRow)).isEqualTo(20);

        // product attached directly to the parent -> the parent's own row, null parent fields
        Map<String, Object> clothingRow = rowFor(rows, "Clothing");
        assertThat(clothingRow.get("parentid")).isNull();
        assertThat(clothingRow.get("parentname")).isNull();
        assertThat(revenue(clothingRow)).isEqualTo(100);

        Map<String, Object> shoesRow = rowFor(rows, "Shoes");
        assertThat(shoesRow.get("parentid")).isNull();
        assertThat(shoesRow.get("parentname")).isNull();
        assertThat(revenue(shoesRow)).isEqualTo(15);

        // parts add up to the whole: 20 + 100 + 15 == 135
        long displayedTotal = rows.stream().mapToLong(RevenueByCategoryIntegrationTest::revenue).sum();
        assertThat(displayedTotal).isEqualTo(135);
    }

    @Test
    void rows_orderedByRevenueDescending() {
        Category clothing = category("Clothing", null);
        Category shirts = category("Shirts", clothing);
        Category shoes = category("Shoes", null);

        order("RBC-2",
                item(product("ShirtB", "10", shirts), 2, "10"),   // 20
                item(product("GiftB", "100", clothing), 1, "100"), // 100
                item(product("SneakerB", "5", shoes), 3, "5"));    // 15

        List<Map<String, Object>> rows = orderRepository.getRevenueByCategory();

        List<Long> revenues = rows.stream().map(RevenueByCategoryIntegrationTest::revenue).toList();
        assertThat(revenues).isSortedAccordingTo((a, b) -> Long.compare(b, a));
        assertThat(rows.get(0).get("categoryname")).isEqualTo("Clothing");
    }
}
