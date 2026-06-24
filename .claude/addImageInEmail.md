Your productRows string is built in OrderService, so that's where we add the image — the email template already
has an .item-img CSS class and .item-row is a flex container, so we just need to include an <img> in each row.

The catch with images in emails: localhost URLs won't load in a real inbox (only in Mailtrap's viewer sometimes). 
But since you're testing on Mailtrap, full URLs will display there. Let me build it so the image shows.

In OrderService, update wherever you build productRows (in updateStatus, and in sendOrderConfirmation if you added it). 
Add the image as the first element of each .item-row:

<java code>
StringBuilder productRows = new StringBuilder();
for (OrderItem item : order.getOrderItems()) {
// Resolve image: snapshot first, then live product, then empty
String imageUrl = nonNull(item.getProductImageUrl())
? item.getProductImageUrl()
: (item.getProduct() != null ? item.getProduct().getImageUrl() : null);

    String fullImageUrl = "";
    if (imageUrl != null && !imageUrl.isBlank()) {
        fullImageUrl = imageUrl.startsWith("http") ? imageUrl : appBaseUrl + imageUrl;
    }

    String imageHtml = fullImageUrl.isBlank()
            ? "<div class=\"item-img\"></div>"
            : "<img src=\"%s\" alt=\"\" class=\"item-img\" />".formatted(fullImageUrl);

    String productName = item.getProductName() != null
            ? item.getProductName()
            : (item.getProduct() != null ? item.getProduct().getName() : "—");

    productRows.append("""
        <div class="item-row">
            %s
            <div>
                <p class="item-name">%s</p>
                <p class="item-qty">Qty: %d × %s RSD</p>
            </div>
            <span class="item-price">%s RSD</span>
        </div>
        """.formatted(
            imageHtml,
            productName,
            item.getQuantity(),
            item.getPriceAtPurchase(),
            item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))
    ));
}
</java code>

This uses the snapshot fields we just added (getProductImageUrl(), getProductName()) with a fallback to the live 
product — so it works for both new orders and old ones, and survives product deletion.

You need the base URL in OrderService. Add it if not present:

@Value("${app.base-url:http://localhost:8080}")
private String appBaseUrl;

The .item-img CSS class already exists in your template (width: 56px; height: 56px; background: #f5f5f5; 
object-fit: contain;), so the image will be sized correctly. The .item-row is display: flex; align-items: center; 
gap: 16px; so the image sits left of the name, the name/qty in the middle, 
and the price pushed right by margin-left: auto — exactly the layout you want.

Two notes:
Email image rendering. In Mailtrap's viewer the image should show if appBaseUrl is a reachable URL. 
If your appBaseUrl is http://localhost:8080, the image won't load in a real email client (and may not in Mailtrap either)
because localhost isn't reachable from outside. For testing it may work in Mailtrap; for production you'd set appBaseUrl
to your deployed domain so the image URL is publicly reachable. If you want images guaranteed to render even on localhost,
the alternative is embedding them inline via CID attachment (like we discussed for the broadcast image) — more complex
but works everywhere. For now, the URL approach is simplest.

display: flex in emails. Some email clients (notably older Outlook) don't support flexbox, so the layout could fall 
back to stacked. Mailtrap and modern clients (Gmail, Apple Mail) handle it fine. If you need bulletproof email 
layout across all clients, tables are the traditional approach — but for your testing and most modern inboxes, 
the current flex layout with the image added will look right.
