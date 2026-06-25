# Feature: Clothing Size Variants (S / M / L / XL) with Per-Size Stock

NoPressure Wear — add size selection with independent stock per size. Each product carries a set of sizes (S, M, L, XL), each with its own stock quantity. Customers must pick an in-stock size before adding to cart; checkout decrements that size's stock; orders snapshot the chosen size.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, React + Vite + Tailwind, react-i18next (EN/SR). Package root: `rs.nopressurewear`.

> **Design:** a `product_variant` row per (product, size). Product-level `stock_quantity` is replaced in the ordering flow by the **sum of size stocks** (keep the old column for display/back-compat or drop it — see §9). Sizes are a fixed enum `S, M, L, XL` (extendable).

---

## 1. Database — Flyway migration

```sql
CREATE TABLE product_variant (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    size            VARCHAR(10) NOT NULL,
    stock_quantity  INTEGER NOT NULL DEFAULT 0,
    sku             VARCHAR(100),
    CONSTRAINT uq_product_size UNIQUE (product_id, size)
);

CREATE INDEX idx_product_variant_product ON product_variant(product_id);

-- Backfill: give every existing product the 4 sizes.
-- Splits the current product.stock_quantity evenly across S/M/L/XL (remainder to S).
INSERT INTO product_variant (product_id, size, stock_quantity)
SELECT p.id, s.size,
       CASE WHEN s.size = 'S'
            THEN (COALESCE(p.stock_quantity,0) / 4) + (COALESCE(p.stock_quantity,0) % 4)
            ELSE (COALESCE(p.stock_quantity,0) / 4)
       END
FROM product p
CROSS JOIN (VALUES ('S'),('M'),('L'),('XL')) AS s(size);
```

> Adjust the backfill if you'd rather start all sizes at 0 and let admins set real stock.

---

## 2. Size enum

```java
package rs.nopressurewear.model;

public enum ProductSize {
    S, M, L, XL
}
```

---

## 3. Entity — ProductVariant.java

```java
package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private ProductSize size;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "sku")
    private String sku;
}
```

### Product.java — add the collection

```java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private List<ProductVariant> variants = new ArrayList<>();
```

> Keep the existing `stockQuantity` field on `Product` for now (display/back-compat). The ordering flow will use per-size stock instead (§7).

---

## 4. Repository — ProductVariantRepository.java

```java
package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import rs.nopressurewear.model.ProductSize;
import rs.nopressurewear.model.ProductVariant;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByProductIdAndSize(Long productId, ProductSize size);

    // Pessimistic lock to prevent overselling under concurrent checkout
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductVariant> findWithLockByProductIdAndSize(Long productId, ProductSize size);

    void deleteByProductId(Long productId);
}
```

---

## 5. DTOs

### ProductVariantRequest.java
```java
@Getter @Setter
public class ProductVariantRequest {
    private ProductSize size;
    private Integer stockQuantity;
    private String sku;
}
```

### ProductVariantResponse.java
```java
@Getter @Setter @Builder
public class ProductVariantResponse {
    private Long id;
    private ProductSize size;
    private Integer stockQuantity;
    private String sku;
    private boolean inStock;   // stockQuantity > 0
}
```

### ProductRequest.java — accept the list
```java
private List<ProductVariantRequest> variants;
```

### ProductResponse.java — return the list + total stock
```java
private List<ProductVariantResponse> variants;
private Integer totalStock;   // sum of variant stock, convenience for UI
```

---

## 6. ProductService — manage variants on create/update + map response

### Create / update: replace variants from the request

```java
private void applyVariants(Product product, List<ProductVariantRequest> requested) {
    // clear existing (orphanRemoval handles deletes), then add from request
    product.getVariants().clear();
    if (requested != null) {
        for (ProductVariantRequest v : requested) {
            if (v.getSize() == null) continue;
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .size(v.getSize())
                    .stockQuantity(v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sku(v.getSku())
                    .build();
            product.getVariants().add(variant);
        }
    }
}
```

Call `applyVariants(product, request.getVariants());` in both `create` and `update` **before** `productRepository.save(product)` (cascade persists the variants).

### toResponse: include variants + total

```java
List<ProductVariantResponse> variantResponses = product.getVariants().stream()
        .sorted(Comparator.comparing(v -> v.getSize().ordinal())) // S,M,L,XL order
        .map(v -> ProductVariantResponse.builder()
                .id(v.getId())
                .size(v.getSize())
                .stockQuantity(v.getStockQuantity())
                .sku(v.getSku())
                .inStock(v.getStockQuantity() != null && v.getStockQuantity() > 0)
                .build())
        .toList();

int totalStock = product.getVariants().stream()
        .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
        .sum();

// add to the builder:
.variants(variantResponses)
.totalStock(totalStock)
```

---

## 7. Cart & Checkout — carry and decrement per-size stock

### 7.1 CartItem — store the chosen size

```sql
-- Flyway
ALTER TABLE cart_item ADD COLUMN size VARCHAR(10);
```
```java
// CartItem.java
@Enumerated(EnumType.STRING)
@Column(name = "size")
private ProductSize size;
```
The "add to cart" request must include the chosen size. Two cart lines for the same product but different sizes are **separate** items (match on product + size when merging quantities).

### 7.2 Add-to-cart — validate size + stock

```java
public CartResponse addToCart(Long userId, Long productId, ProductSize size, int quantity) {
    if (size == null) throw new RuntimeException("Size is required");

    ProductVariant variant = productVariantRepository
            .findByProductIdAndSize(productId, size)
            .orElseThrow(() -> new ResourceNotFoundException("Size not available for this product"));

    if (variant.getStockQuantity() < quantity) {
        throw new RuntimeException("Not enough stock for size " + size);
    }
    // ... find-or-create cart line matching (product, size); add quantity ...
}
```

### 7.3 Checkout — decrement the variant stock (with lock)

In `checkout` and `guestCheckout`, for each cart item:

```java
ProductVariant variant = productVariantRepository
        .findWithLockByProductIdAndSize(productId, item.getSize())
        .orElseThrow(() -> new RuntimeException("Size no longer available"));

if (variant.getStockQuantity() < item.getQuantity()) {
    throw new RuntimeException("Insufficient stock for " + product.getName() + " (" + item.getSize() + ")");
}
variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
productVariantRepository.save(variant);
```

The pessimistic lock prevents two simultaneous checkouts from overselling the last item.

### 7.4 OrderItem — snapshot the size

```sql
-- Flyway
ALTER TABLE order_item ADD COLUMN size VARCHAR(10);
```
```java
// OrderItem.java
@Enumerated(EnumType.STRING)
@Column(name = "size")
private ProductSize size;
```
Set `.size(item.getSize())` when building the `OrderItem` at checkout. Include it in `OrderItemResponse` and show it in order views and emails (e.g. "Size: M").

### 7.5 Cancel → restore per-size stock

In `updateStatus`, when moving to `CANCELLED` (and the guard `previousStatus != CANCELLED`), restore to the **variant**, not the product:

```java
for (OrderItem item : order.getOrderItems()) {
    if (item.getSize() != null && item.getProduct() != null) {
        productVariantRepository
            .findByProductIdAndSize(item.getProduct().getId(), item.getSize())
            .ifPresent(v -> {
                v.setStockQuantity(v.getStockQuantity() + item.getQuantity());
                productVariantRepository.save(v);
            });
    }
}
```
Apply the reverse (deduct again) in the un-cancel branch, mirroring the existing logic.

---

## 8. Frontend

### 8.1 Admin product form — manage sizes + per-size stock

State holds an array of the 4 sizes with stock:

```jsx
const ALL_SIZES = ['S', 'M', 'L', 'XL'];

// in formData
variants: ALL_SIZES.map(size => ({ size, stockQuantity: 0, sku: '' })),
```

UI (a row per size):

```jsx
<div className="md:col-span-2">
    <label className={labelClass}>{t('admin.sizesAndStock')}</label>
    <div className="space-y-2">
        {formData.variants.map((v, idx) => (
            <div key={v.size} className="flex items-center gap-3">
                <span className="w-10 text-sm font-semibold text-black">{v.size}</span>
                <input
                    type="number"
                    min="0"
                    value={v.stockQuantity}
                    onChange={(e) => {
                        const val = e.target.value === '' ? '' : parseInt(e.target.value, 10);
                        setFormData(prev => ({
                            ...prev,
                            variants: prev.variants.map((x, i) => i === idx ? { ...x, stockQuantity: val } : x)
                        }));
                    }}
                    placeholder={t('product.stock')}
                    className="w-28 border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black"
                />
                <input
                    type="text"
                    value={v.sku || ''}
                    onChange={(e) => {
                        const val = e.target.value;
                        setFormData(prev => ({
                            ...prev,
                            variants: prev.variants.map((x, i) => i === idx ? { ...x, sku: val } : x)
                        }));
                    }}
                    placeholder={t('admin.sku')}
                    className="flex-1 border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black"
                />
            </div>
        ))}
    </div>
    <p className="text-xs text-gray-400 mt-1">{t('admin.sizesHint')}</p>
</div>
```

On `handleEdit`, map the product's returned variants back into the 4-row shape (fill missing sizes with 0 so all four always show):

```jsx
variants: ALL_SIZES.map(size => {
    const found = product.variants?.find(v => v.size === size);
    return { size, stockQuantity: found?.stockQuantity ?? 0, sku: found?.sku ?? '' };
}),
```

On submit, coerce empty stock to 0 before sending.

### 8.2 Product detail page — size selector (required)

```jsx
const [selectedSize, setSelectedSize] = useState(null);

<div className="mb-6">
    <p className="text-xs font-semibold uppercase tracking-wide text-black mb-3">
        {t('product.size')}
    </p>
    <div className="flex gap-2 flex-wrap">
        {product.variants.map(v => {
            const disabled = !v.inStock;
            const selected = selectedSize === v.size;
            return (
                <button
                    key={v.size}
                    onClick={() => !disabled && setSelectedSize(v.size)}
                    disabled={disabled}
                    className={`min-w-[48px] px-3 py-2 text-sm font-semibold border transition-colors
                        ${selected ? 'bg-black text-white border-black'
                                   : 'border-gray-300 text-black hover:border-black'}
                        ${disabled ? 'opacity-30 cursor-not-allowed line-through' : ''}`}
                    title={disabled ? t('product.sizeSoldOut') : ''}
                >
                    {v.size}
                </button>
            );
        })}
    </div>
    {!selectedSize && (
        <p className="text-xs text-gray-400 mt-2">{t('product.selectSizePrompt')}</p>
    )}
</div>
```

Gate add-to-cart on size selection:

```jsx
<button
    onClick={handleAddToCart}
    disabled={!selectedSize || addingToCart}
    className="... disabled:opacity-30"
>
    {!selectedSize ? t('product.selectSizeFirst') : t('product.addToCart')}
</button>
```

Pass `selectedSize` in the add-to-cart call:

```jsx
await addToCart(product.id, selectedSize, quantity);
```

### 8.3 API (cartApi.js)

```javascript
export const addToCart = (productId, size, quantity = 1) =>
    axiosInstance.post('/cart/items', { productId, size, quantity });
```
(adjust to your actual endpoint shape)

### 8.4 Cart + order views — show the size

Render the size next to the product name in the cart, order detail, and admin order views:

```jsx
<p className="text-xs text-gray-400">{item.size && `${t('product.size')}: ${item.size}`}</p>
```

### 8.5 Email — show size in item rows

In `buildOrderItemRowsHtml`, add the size under the name:

```java
<p class="item-qty">%s · Qty: %d × %s RSD</p>
```
formatted with `item.getSize() != null ? "Size " + item.getSize() : ""` (and quantity, price).

---

## 9. Product-level stock field — decision

You now have per-size stock. For the old `product.stock_quantity`:
- **Option A (recommended):** keep it as a read-only computed total (sum of variants) for display/reports; stop using it for ordering. Set it = sum on save, or compute on the fly (`totalStock`).
- **Option B:** drop the column entirely and use `totalStock` everywhere. More invasive (low-stock report, admin table, filters must switch to the sum).

If you keep it, update it on save:
```java
product.setStockQuantity(
    product.getVariants().stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum()
);
```

Low-stock report: switch the threshold check to per-size (alert when any size is low) or to the total, depending on what's useful.

---

## 10. i18n keys (en.json / sr.json)

```jsonc
// en.json
"product": {
    "size": "Size",
    "selectSizePrompt": "Please select a size",
    "selectSizeFirst": "Select a size",
    "selectSizeFirst_short": "Select size",
    "sizeSoldOut": "Sold out"
},
"admin": {
    "sizesAndStock": "Sizes & Stock",
    "sizesHint": "Set available stock per size. 0 = that size is sold out."
}

// sr.json
"product": {
    "size": "Veličina",
    "selectSizePrompt": "Molimo izaberite veličinu",
    "selectSizeFirst": "Izaberite veličinu",
    "selectSizeFirst_short": "Izaberite veličinu",
    "sizeSoldOut": "Rasprodato"
},
"admin": {
    "sizesAndStock": "Veličine i zalihe",
    "sizesHint": "Postavite zalihe po veličini. 0 = ta veličina je rasprodata."
}
```

---

## 11. Edge cases & notes

- **Size required:** enforce on both frontend (disable add-to-cart) and backend (reject add-to-cart/checkout with null size) — never trust the client alone.
- **Sold-out size:** disable the button, show strikethrough; backend rejects ordering a 0-stock size.
- **Concurrency:** the pessimistic lock in checkout (`findWithLockByProductIdAndSize`) prevents overselling the last unit.
- **Cart lines:** same product + different size = different cart items; same product + same size = merge quantities.
- **Deleting a product:** `product_variant` has `ON DELETE CASCADE`, so variants are removed automatically (consistent with the order_item snapshot approach that keeps history).
- **Old orders:** `order_item.size` is null for pre-migration orders — render gracefully (omit the size line when null).
- **Adding sizes later:** the enum is extensible (add `XXL`); the admin form's `ALL_SIZES` array and i18n drive the UI.

---

## 12. Checklist

- [ ] Flyway: `product_variant` table + backfill
- [ ] Flyway: `cart_item.size`, `order_item.size`
- [ ] `ProductSize` enum, `ProductVariant` entity, repository (with lock query)
- [ ] DTOs: variant request/response, product request/response include variants + totalStock
- [ ] ProductService: applyVariants on create/update, variants + totalStock in response
- [ ] Add-to-cart requires + validates size; cart line keyed by (product, size)
- [ ] Checkout decrements per-size stock with lock
- [ ] Cancel restores per-size stock (and un-cancel re-deducts)
- [ ] OrderItem snapshots size; shown in order views + email
- [ ] Admin form: 4 size rows with per-size stock + sku
- [ ] Product page: required size selector, sold-out sizes disabled, add-to-cart gated
- [ ] Cart/order/admin views show size
- [ ] Product-level stock decision applied (computed total or dropped)
- [ ] i18n keys (EN + SR)
- [ ] Verified: required size, sold-out disabled, no overselling under concurrency, cancel restores
