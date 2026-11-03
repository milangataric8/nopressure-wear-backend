package rs.nopressurewear.catalog.internal.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.catalog.api.Gender;
import rs.nopressurewear.catalog.api.ProductSize;
import rs.nopressurewear.catalog.internal.domain.Product;
import rs.nopressurewear.catalog.internal.domain.ProductVariant;
import rs.nopressurewear.catalog.internal.domain.ProductColorVariant;
import rs.nopressurewear.catalog.internal.persistence.ProductRepository;
import rs.nopressurewear.catalog.internal.persistence.ProductColorVariantRepository;
import rs.nopressurewear.catalog.internal.persistence.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class CatalogModelIntegrationTest {
    @Autowired CatalogModelService service;
    @Autowired ProductRepository products;
    @Autowired ProductColorVariantRepository links;
    @Autowired ProductVariantRepository sizes;

    private Product product(String name, String color, boolean active, int price) {
        return products.save(Product.builder().name(name).sku(name + color).colorName(color)
                .price(BigDecimal.valueOf(price)).isActive(active).gender(Gender.UNISEX).build());
    }
    private void link(Product a, Product b) {
        links.save(ProductColorVariant.builder().product(a).variant(b).build());
    }
    private void stock(Product product, ProductSize size, int quantity) {
        sizes.save(ProductVariant.builder().product(product).size(size).stockQuantity(quantity).build());
    }
    @Test void groupsTransitiveAndReciprocalLinksBeforePagingWithoutMergingIdenticalNames() {
        var a = product("Catalog test Alpha", "White", true, 2800);
        var b = product("Catalog test Alpha", "Black", true, 2900);
        var c = product("Catalog test Alpha", "Green", true, 3000);
        var standalone = product("Catalog test Alpha", "Navy", true, 3100);
        var hidden = product("Catalog test Hidden", "Beige", false, 1000);
        link(a,b); link(b,a); link(b,c); link(c,hidden); link(hidden,standalone);
        var first = service.search(null,"Catalog test",null,null,null,null,null,null,null,PageRequest.of(0,1,Sort.by("name")));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(1);
        assertThat(first.getContent().getFirst().options()).extracting(p -> p.getId()).containsExactly(a.getId(),b.getId(),c.getId());
        var second = service.search(null,"Catalog test",null,null,null,null,null,null,null,PageRequest.of(1,1,Sort.by("name")));
        assertThat(second.getContent().getFirst().options()).extracting(p -> p.getId()).containsExactly(standalone.getId());
    }
    @Test void colorAndSizeMustMatchSameActiveOptionAndSortUsesMatchingPrices() {
        var white = product("Catalog filter Alpha", "White", true, 2800);
        var green = product("Catalog filter Alpha", "Green", true, 3200);
        var other = product("Catalog filter Beta", "Green", true, 3000);
        link(white,green); stock(white,ProductSize.M,10); stock(green,ProductSize.M,0); stock(green,ProductSize.L,10); stock(other,ProductSize.M,10);
        var result = service.search(null,"Catalog filter",null,null,null,"Green",null,null,List.of(ProductSize.M),PageRequest.of(0,12));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().options().getFirst().getId()).isEqualTo(other.getId());
        var sorted = service.search(null,"Catalog filter",null,null,null,"Green",null,null,null,PageRequest.of(0,12,Sort.by(Sort.Direction.DESC,"price")));
        assertThat(sorted.getContent()).extracting(m -> m.options().getFirst().getId()).containsExactly(green.getId(),other.getId());
        var empty = service.search(null,"Catalog filter",null,BigDecimal.valueOf(100),null,null,null,null,null,PageRequest.of(0,12));
        assertThat(empty.getTotalElements()).isZero();
        assertThat(empty.getContent()).isEmpty();
    }
}
