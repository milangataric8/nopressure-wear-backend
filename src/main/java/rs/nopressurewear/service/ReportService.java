package rs.nopressurewear.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import rs.nopressurewear.model.Order;
import rs.nopressurewear.model.OrderItem;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.lowagie.text.Element.ALIGN_CENTER;
import static com.lowagie.text.Element.ALIGN_RIGHT;
import static com.lowagie.text.Font.HELVETICA;
import static com.lowagie.text.PageSize.A4;
import static java.awt.Color.*;
import static java.awt.Font.BOLD;
import static java.awt.Font.ITALIC;
import static java.awt.Frame.NORMAL;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;
import static org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND;
import static org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER;

@Service
public class ReportService {

    private static final Font TITLE_FONT = new Font(HELVETICA, 18, BOLD, BLACK);
    private static final Font SUBTITLE_FONT = new Font(HELVETICA, 10, NORMAL, GRAY);
    private static final Font HEADER_FONT = new Font(HELVETICA, 9, BOLD, WHITE);
    static final Font CELL_FONT = new Font(HELVETICA, 8, NORMAL, BLACK);
    static final Font BOLD_CELL_FONT = new Font(HELVETICA, 8, BOLD, BLACK);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    Map<String, String> getTranslations(String lang) {
        Map<String, String> t = new HashMap<>();
        if ("sr".equals(lang)) {
            t.put("ordersReport", "Izveštaj porudžbina");
            t.put("productsReport", "Izveštaj proizvoda");
            t.put("customersReport", "Izveštaj kupaca");
            t.put("lowStockReport", "Izveštaj niskih zaliha");
            t.put("generated", "Generisano");
            t.put("total", "Ukupno");

            t.put("orderCode", "Šifra");
            t.put("customer", "Kupac");
            t.put("email", "Email");
            t.put("amount", "Iznos");
            t.put("discount", "Popust");
            t.put("coupon", "Kupon");
            t.put("status", "Status");
            t.put("paymentStatus", "Status plaćanja");
            t.put("paymentMethod", "Način plaćanja");
            t.put("date", "Datum");
            t.put("orders", "porudžbina");
            t.put("totalRevenue", "Ukupan prihod");
            t.put("street", "Ulica");
            t.put("city", "Grad");
            t.put("postalCode", "Poštanski broj");
            t.put("country", "Država");

            t.put("name", "Naziv");
            t.put("sku", "SKU");
            t.put("price", "Cena");
            t.put("discountPrice", "Cena sa popustom");
            t.put("discountPercentage", "Popust %");
            t.put("stock", "Zaliha");
            t.put("brand", "Brend");
            t.put("color", "Boja");
            t.put("material", "Materijal");
            t.put("category", "Kategorija");
            t.put("salesCount", "Prodato");
            t.put("avgRating", "Prosečna ocena");
            t.put("products", "proizvoda");
            t.put("totalStockValue", "Ukupna vrednost zaliha");
            t.put("active", "Aktivno");
            t.put("inactive", "Neaktivno");

            t.put("totalSpent", "Ukupno potrošeno");
            t.put("customers", "kupaca");

            t.put("threshold", "Prag");
            t.put("units", "komada");
        } else {
            t.put("ordersReport", "Orders Report");
            t.put("productsReport", "Product Catalog Report");
            t.put("customersReport", "Customer Report");
            t.put("lowStockReport", "Low Stock Report");
            t.put("generated", "Generated");
            t.put("total", "Total");

            t.put("orderCode", "Order Code");
            t.put("customer", "Customer");
            t.put("email", "Email");
            t.put("amount", "Amount");
            t.put("discount", "Discount");
            t.put("coupon", "Coupon");
            t.put("status", "Status");
            t.put("paymentStatus", "Payment Status");
            t.put("paymentMethod", "Payment Method");
            t.put("date", "Date");
            t.put("orders", "orders");
            t.put("totalRevenue", "Total Revenue");
            t.put("street", "Street");
            t.put("city", "City");
            t.put("postalCode", "Postal Code");
            t.put("country", "Country");

            t.put("name", "Name");
            t.put("sku", "SKU");
            t.put("price", "Price");
            t.put("discountPrice", "Discount Price");
            t.put("discountPercentage", "Discount %");
            t.put("stock", "Stock");
            t.put("brand", "Brand");
            t.put("color", "Color");
            t.put("material", "Material");
            t.put("category", "Category");
            t.put("salesCount", "Sales Count");
            t.put("avgRating", "Avg Rating");
            t.put("products", "products");
            t.put("totalStockValue", "Total Stock Value");
            t.put("active", "Active");
            t.put("inactive", "Inactive");

            t.put("totalSpent", "Total Spent");
            t.put("customers", "customers");

            t.put("threshold", "Threshold");
            t.put("units", "units");
        }
        return t;
    }

    void addTitle(Document doc, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, TITLE_FONT);
        title.setSpacingAfter(5);
        doc.add(title);
    }

    void addSubtitle(Document doc, String text) throws DocumentException {
        Paragraph sub = new Paragraph(text, SUBTITLE_FONT);
        sub.setSpacingAfter(10);
        doc.add(sub);
    }

    void addSummary(Document doc, String text) throws DocumentException {
        Paragraph summary = new Paragraph(text, new Font(HELVETICA, 11, BOLD));
        summary.setSpacingBefore(15);
        summary.setAlignment(ALIGN_RIGHT);
        doc.add(summary);
    }

    void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BLACK);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }

    void addBoldCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BOLD_CELL_FONT));
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }

    String formatPrice(BigDecimal price) {
        if (price == null) return "0 RSD";
        return String.format("%,.0f RSD", price);
    }

    CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        style.setFillPattern(SOLID_FOREGROUND);
        style.setAlignment(CENTER);
        return style;
    }

    CellStyle createBoldStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public byte[] generateInvoicePdf(Order order, String lang) throws DocumentException {
        Map<String, String> t = getInvoiceTranslations(lang);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, t.get("invoice"));
        addSubtitle(document, t.get("invoiceNumber") + ": #" + order.getOrderCode());
        addSubtitle(document, t.get("date") + ": " + order.getCreatedAt().format(DATE_FORMAT));

        document.add(new Paragraph(" "));

        PdfPTable customerTable = new PdfPTable(2);
        customerTable.setWidthPercentage(100);
        customerTable.setSpacingBefore(10);

        PdfPCell billToCell = new PdfPCell();
        billToCell.setBorderWidth(0);
        billToCell.addElement(new Paragraph(t.get("billTo"), BOLD_CELL_FONT));
        billToCell.addElement(new Paragraph(order.getCustomerFullName(), CELL_FONT));
        billToCell.addElement(new Paragraph(order.getCustomerEmail(), CELL_FONT));
        customerTable.addCell(billToCell);

        PdfPCell shipToCell = new PdfPCell();
        shipToCell.setBorderWidth(0);
        shipToCell.addElement(new Paragraph(t.get("shipTo"), BOLD_CELL_FONT));
        if (nonNull(order.getShippingAddress())) {
            shipToCell.addElement(new Paragraph(order.getShippingAddress().getStreet(), CELL_FONT));
            shipToCell.addElement(new Paragraph(
                    order.getShippingAddress().getCity() + ", " +
                            order.getShippingAddress().getPostalCode(), CELL_FONT));
            shipToCell.addElement(new Paragraph(order.getShippingAddress().getCountry(), CELL_FONT));
        }
        customerTable.addCell(shipToCell);
        document.add(customerTable);

        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(new float[]{2, 2, 2});
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(5);

        addInfoCell(infoTable, t.get("paymentMethod"), nonNull(order.getPaymentMethod())
                ? ("CARD".equals(order.getPaymentMethod()) ? t.get("card") : t.get("cod"))
                : "—");
        addInfoCell(infoTable, t.get("paymentStatus"), nonNull(order.getPaymentStatus())
                ? ("PAID".equals(order.getPaymentStatus()) ? t.get("paid") : t.get("pending"))
                : "—");
        addInfoCell(infoTable, t.get("orderStatus"), order.getStatus().name());
        document.add(infoTable);

        document.add(new Paragraph(" "));

        PdfPTable itemsTable = new PdfPTable(new float[]{1, 4, 1.5f, 1.5f, 2});
        itemsTable.setWidthPercentage(100);
        itemsTable.setSpacingBefore(10);

        addHeaderCell(itemsTable, "#");
        addHeaderCell(itemsTable, t.get("product"));
        addHeaderCell(itemsTable, t.get("quantity"));
        addHeaderCell(itemsTable, t.get("unitPrice"));
        addHeaderCell(itemsTable, t.get("subtotal"));

        for (int i = 0; i < order.getOrderItems().size(); i++) {
            OrderItem item = order.getOrderItems().get(i);
            addCell(itemsTable, String.valueOf(i + 1));
            addCell(itemsTable, item.getProduct().getName());
            addCell(itemsTable, String.valueOf(item.getQuantity()));
            addCell(itemsTable, formatPrice(item.getPriceAtPurchase()));
            addBoldCell(itemsTable, formatPrice(
                    item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }

        document.add(itemsTable);

        PdfPTable totalsTable = new PdfPTable(new float[]{6, 2});
        totalsTable.setWidthPercentage(100);
        totalsTable.setSpacingBefore(15);

        BigDecimal subtotal = order.getOrderItems().stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(ZERO, BigDecimal::add);

        addTotalRow(totalsTable, t.get("subtotalLabel"), formatPrice(subtotal));

        if (nonNull(order.getDiscountAmount()) && order.getDiscountAmount().compareTo(ZERO) > 0) {
            createDiscountPart(order, t, totalsTable);
        }

        BigDecimal deliveryFeeValue = nonNull(order.getDeliveryFee()) ? order.getDeliveryFee() : ZERO;
        String deliveryValue = deliveryFeeValue.compareTo(ZERO) == 0 ? t.get("free") : formatPrice(deliveryFeeValue);
        addTotalRow(totalsTable, t.get("delivery"), deliveryValue);

        PdfPCell totalLabel = new PdfPCell(new Phrase(t.get("totalLabel"), new Font(HELVETICA, 11, BOLD)));
        totalLabel.setBorderWidth(0);
        totalLabel.setBorderWidthTop(1);
        totalLabel.setPaddingTop(8);
        totalLabel.setHorizontalAlignment(ALIGN_RIGHT);
        totalsTable.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(
                new Phrase(formatPrice(order.getTotalAmount()), new Font(HELVETICA, 11, BOLD)));
        totalValue.setBorderWidth(0);
        totalValue.setBorderWidthTop(1);
        totalValue.setPaddingTop(8);
        totalValue.setHorizontalAlignment(ALIGN_RIGHT);
        totalsTable.addCell(totalValue);

        document.add(totalsTable);

        Paragraph footer = new Paragraph(t.get("thankYou"), new Font(HELVETICA, 10, ITALIC, GRAY));
        footer.setSpacingBefore(30);
        footer.setAlignment(ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void createDiscountPart(Order order, Map<String, String> t, PdfPTable totalsTable) {
        String discountText = t.get("discountLabel");
        if (nonNull(order.getCouponCode())) {
            discountText += " (" + order.getCouponCode() + ")";
        }
        addTotalRow(totalsTable, discountText, "-" + formatPrice(order.getDiscountAmount()));
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(LIGHT_GRAY);
        cell.setPadding(8);
        cell.addElement(new Paragraph(label, new Font(HELVETICA, 7, NORMAL, GRAY)));
        cell.addElement(new Paragraph(value, new Font(HELVETICA, 9, BOLD)));
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(HELVETICA, 9, NORMAL, GRAY)));
        labelCell.setBorderWidth(0);
        labelCell.setPaddingTop(4);
        labelCell.setHorizontalAlignment(ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, new Font(HELVETICA, 9, BOLD)));
        valueCell.setBorderWidth(0);
        valueCell.setPaddingTop(4);
        valueCell.setHorizontalAlignment(ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private Map<String, String> getInvoiceTranslations(String lang) {
        Map<String, String> t = new HashMap<>();
        if ("sr".equals(lang)) {
            t.put("invoice", "RAČUN");
            t.put("invoiceNumber", "Broj računa");
            t.put("date", "Datum");
            t.put("billTo", "KUPAC");
            t.put("shipTo", "ADRESA DOSTAVE");
            t.put("product", "Proizvod");
            t.put("quantity", "Količina");
            t.put("unitPrice", "Cena");
            t.put("subtotal", "Iznos");
            t.put("subtotalLabel", "Međuzbir");
            t.put("discountLabel", "Popust");
            t.put("delivery", "Dostava");
            t.put("free", "Besplatno");
            t.put("totalLabel", "UKUPNO");
            t.put("paymentMethod", "Način plaćanja");
            t.put("paymentStatus", "Status plaćanja");
            t.put("orderStatus", "Status porudžbine");
            t.put("card", "Kartica");
            t.put("cod", "Pouzeće");
            t.put("paid", "Plaćeno");
            t.put("pending", "Na čekanju");
            t.put("thankYou", "Hvala Vam na kupovini!");
        } else {
            t.put("invoice", "INVOICE");
            t.put("invoiceNumber", "Invoice Number");
            t.put("date", "Date");
            t.put("billTo", "BILL TO");
            t.put("shipTo", "SHIP TO");
            t.put("product", "Product");
            t.put("quantity", "Qty");
            t.put("unitPrice", "Unit Price");
            t.put("subtotal", "Subtotal");
            t.put("subtotalLabel", "Subtotal");
            t.put("discountLabel", "Discount");
            t.put("delivery", "Delivery");
            t.put("free", "Free");
            t.put("totalLabel", "TOTAL");
            t.put("paymentMethod", "Payment Method");
            t.put("paymentStatus", "Payment Status");
            t.put("orderStatus", "Order Status");
            t.put("card", "Card");
            t.put("cod", "Cash on Delivery");
            t.put("paid", "Paid");
            t.put("pending", "Pending");
            t.put("thankYou", "Thank you for your purchase!");
        }
        return t;
    }
}
