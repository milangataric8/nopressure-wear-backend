package rs.webshop.webshop_core.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.model.*;
import rs.webshop.webshop_core.repository.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lowagie.text.Element.ALIGN_RIGHT;
import static com.lowagie.text.Font.HELVETICA;
import static com.lowagie.text.PageSize.A4;
import static java.awt.Color.*;
import static java.awt.Font.BOLD;
import static java.awt.Frame.NORMAL;
import static java.math.BigDecimal.ZERO;
import static org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND;
import static org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private static final Font TITLE_FONT = new Font(HELVETICA, 18, BOLD, BLACK);
    private static final Font SUBTITLE_FONT = new Font(HELVETICA, 10, NORMAL, GRAY);
    private static final Font HEADER_FONT = new Font(HELVETICA, 9, BOLD, WHITE);
    private static final Font CELL_FONT = new Font(HELVETICA, 8, NORMAL, BLACK);
    private static final Font BOLD_CELL_FONT = new Font(HELVETICA, 8, BOLD, BLACK);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Map<String, String> getTranslations(String lang) {
        Map<String, String> t = new HashMap<>();
        if ("sr".equals(lang)) {
            // Report titles
            t.put("ordersReport", "Izveštaj porudžbina");
            t.put("productsReport", "Izveštaj proizvoda");
            t.put("customersReport", "Izveštaj kupaca");
            t.put("lowStockReport", "Izveštaj niskih zaliha");
            t.put("generated", "Generisano");
            t.put("total", "Ukupno");

            // Orders
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

            // Products
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

            // Customers
            t.put("totalSpent", "Ukupno potrošeno");
            t.put("customers", "kupaca");

            // Low stock
            t.put("threshold", "Prag");
            t.put("units", "komada");
        } else {
            // English defaults
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

    // ==================== ORDERS ====================

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateOrdersPdf(String lang) throws DocumentException {
        Map<String, String> t = getTranslations(lang);
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, t.get("ordersReport"));
        addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + orders.size() + " " + t.get("orders"));

        PdfPTable table = new PdfPTable(new float[]{1, 2, 3, 2, 2, 2, 2, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        addHeaderCell(table, "#");
        addHeaderCell(table, t.get("orderCode"));
        addHeaderCell(table, t.get("customer"));
        addHeaderCell(table, t.get("amount"));
        addHeaderCell(table, t.get("status"));
        addHeaderCell(table, t.get("paymentStatus"));
        addHeaderCell(table, t.get("paymentMethod"));
        addHeaderCell(table, t.get("date"));

        BigDecimal totalRevenue = ZERO;
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            addCell(table, String.valueOf(i + 1));
            addCell(table, order.getOrderCode());
            addCell(table, order.getCustomerFullName());
            addBoldCell(table, formatPrice(order.getTotalAmount()));
            addCell(table, order.getStatus().name());
            addCell(table, order.getPaymentStatus() != null ? order.getPaymentStatus() : "—");
            addCell(table, order.getPaymentMethod() != null ? order.getPaymentMethod() : "—");
            addCell(table, order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_ONLY_FORMAT) : "—");
            totalRevenue = totalRevenue.add(order.getTotalAmount() != null ? order.getTotalAmount() : ZERO);
        }

        document.add(table);
        addSummary(document, t.get("totalRevenue") + ": " + formatPrice(totalRevenue));
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateOrdersExcel(String lang) throws IOException {
        Map<String, String> t = getTranslations(lang);
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("ordersReport"));

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);

        Row header = sheet.createRow(0);
        String[] columns = {"#", t.get("orderCode"), t.get("customer"), t.get("email"), t.get("amount"),
                t.get("discount"), t.get("coupon"), t.get("status"), t.get("paymentStatus"),
                t.get("paymentMethod"), t.get("street"), t.get("city"), t.get("postalCode"),
                t.get("country"), t.get("date")};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        BigDecimal totalRevenue = ZERO;
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(order.getOrderCode());
            row.createCell(2).setCellValue(order.getCustomerFullName());
            row.createCell(3).setCellValue(order.getCustomerEmail());
            row.createCell(4).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0);
            row.createCell(5).setCellValue(order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0);
            row.createCell(6).setCellValue(order.getCouponCode() != null ? order.getCouponCode() : "");
            row.createCell(7).setCellValue(order.getStatus().name());
            row.createCell(8).setCellValue(order.getPaymentStatus() != null ? order.getPaymentStatus() : "");
            row.createCell(9).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
            row.createCell(10).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress().getStreet() : "");
            row.createCell(11).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress().getCity() : "");
            row.createCell(12).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress().getPostalCode() : "");
            row.createCell(13).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress().getCountry() : "");
            row.createCell(14).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FORMAT) : "");
            totalRevenue = totalRevenue.add(order.getTotalAmount() != null ? order.getTotalAmount() : ZERO);
        }

        Row summaryRow = sheet.createRow(orders.size() + 2);
        Cell summaryLabel = summaryRow.createCell(3);
        summaryLabel.setCellValue(t.get("totalRevenue") + ":");
        summaryLabel.setCellStyle(boldStyle);
        Cell summaryValue = summaryRow.createCell(4);
        summaryValue.setCellValue(totalRevenue.doubleValue());
        summaryValue.setCellStyle(boldStyle);

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ==================== PRODUCTS ====================

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateProductsPdf(String lang) throws DocumentException {
        Map<String, String> t = getTranslations(lang);
        List<Product> products = productRepository.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, t.get("productsReport"));
        addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + products.size() + " " + t.get("products"));

        PdfPTable table = new PdfPTable(new float[]{1, 3, 2, 1.5f, 1.5f, 1, 1.5f, 1, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        addHeaderCell(table, "#");
        addHeaderCell(table, t.get("name"));
        addHeaderCell(table, t.get("sku"));
        addHeaderCell(table, t.get("price"));
        addHeaderCell(table, t.get("discountPrice"));
        addHeaderCell(table, t.get("stock"));
        addHeaderCell(table, t.get("brand"));
        addHeaderCell(table, t.get("salesCount"));
        addHeaderCell(table, t.get("status"));

        BigDecimal totalValue = ZERO;
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            addCell(table, String.valueOf(i + 1));
            addCell(table, p.getName());
            addCell(table, p.getSku() != null ? p.getSku() : "—");
            addBoldCell(table, formatPrice(p.getPrice()));
            addCell(table, p.getDiscountPrice() != null ? formatPrice(p.getDiscountPrice()) : "—");
            addCell(table, String.valueOf(p.getStockQuantity()));
            addCell(table, p.getBrand() != null ? p.getBrand() : "—");
            addCell(table, String.valueOf(p.getSalesCount() != null ? p.getSalesCount() : 0));
            addCell(table, p.isActive() ? t.get("active") : t.get("inactive"));
            totalValue = totalValue.add(p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())));
        }

        document.add(table);
        addSummary(document, t.get("totalStockValue") + ": " + formatPrice(totalValue));
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateProductsExcel(String lang) throws IOException {
        Map<String, String> t = getTranslations(lang);
        List<Product> products = productRepository.findAll();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("productsReport"));

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);

        Row header = sheet.createRow(0);
        String[] columns = {"#", t.get("name"), t.get("sku"), t.get("price"), t.get("discountPercentage"),
                t.get("discountPrice"), t.get("stock"), t.get("brand"), t.get("color"),
                t.get("material"), t.get("category"), t.get("salesCount"), t.get("avgRating"), t.get("status")};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        BigDecimal totalValue = ZERO;
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(p.getName());
            row.createCell(2).setCellValue(p.getSku() != null ? p.getSku() : "");
            row.createCell(3).setCellValue(p.getPrice().doubleValue());
            row.createCell(4).setCellValue(p.getDiscountPercentage() != null ? p.getDiscountPercentage().doubleValue() : 0);
            row.createCell(5).setCellValue(p.getDiscountPrice() != null ? p.getDiscountPrice().doubleValue() : 0);
            row.createCell(6).setCellValue(p.getStockQuantity());
            row.createCell(7).setCellValue(p.getBrand() != null ? p.getBrand() : "");
            row.createCell(8).setCellValue(p.getColorName() != null ? p.getColorName() : "");
            row.createCell(9).setCellValue(p.getMaterial() != null ? p.getMaterial() : "");
            row.createCell(10).setCellValue(p.getCategory() != null ? p.getCategory().getName() : "");
            row.createCell(11).setCellValue(p.getSalesCount() != null ? p.getSalesCount() : 0);
            row.createCell(12).setCellValue(p.getAverageRating() != null ? p.getAverageRating().doubleValue() : 0);
            row.createCell(13).setCellValue(p.isActive() ? t.get("active") : t.get("inactive"));
            totalValue = totalValue.add(p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())));
        }

        Row summaryRow = sheet.createRow(products.size() + 2);
        Cell label = summaryRow.createCell(5);
        label.setCellValue(t.get("totalStockValue") + ":");
        label.setCellStyle(boldStyle);
        Cell value = summaryRow.createCell(6);
        value.setCellValue(totalValue.doubleValue());
        value.setCellStyle(boldStyle);

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ==================== CUSTOMERS ====================

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateCustomersPdf(String lang) throws DocumentException {
        Map<String, String> t = getTranslations(lang);
        List<Map<String, Object>> customers = orderRepository.findAllCustomerStats();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, t.get("customersReport"));
        addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + customers.size() + " " + t.get("customers"));

        PdfPTable table = new PdfPTable(new float[]{1, 3, 3, 1.5f, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        addHeaderCell(table, "#");
        addHeaderCell(table, t.get("name"));
        addHeaderCell(table, t.get("email"));
        addHeaderCell(table, t.get("orders"));
        addHeaderCell(table, t.get("totalSpent"));

        for (int i = 0; i < customers.size(); i++) {
            Map<String, Object> c = customers.get(i);
            addCell(table, String.valueOf(i + 1));
            addCell(table, String.valueOf(c.get("name")));
            addCell(table, String.valueOf(c.get("email")));
            addCell(table, String.valueOf(c.get("orders")));
            addBoldCell(table, formatPrice((BigDecimal) c.get("total_spent")));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateCustomersExcel(String lang) throws IOException {
        Map<String, String> t = getTranslations(lang);
        List<Map<String, Object>> customers = orderRepository.findAllCustomerStats();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("customersReport"));

        CellStyle headerStyle = createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] columns = {"#", t.get("name"), t.get("email"), t.get("orders"), t.get("totalSpent")};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < customers.size(); i++) {
            Map<String, Object> c = customers.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(String.valueOf(c.get("name")));
            row.createCell(2).setCellValue(String.valueOf(c.get("email")));
            row.createCell(3).setCellValue(((Number) c.get("orders")).doubleValue());
            row.createCell(4).setCellValue(((BigDecimal) c.get("total_spent")).doubleValue());
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ==================== LOW STOCK ====================

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateLowStockPdf(int threshold, String lang) throws DocumentException {
        Map<String, String> t = getTranslations(lang);
        List<Product> products = productRepository.findByIsActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, t.get("lowStockReport"));
        addSubtitle(document, t.get("threshold") + ": " + threshold + " " + t.get("units")
                + " | " + t.get("total") + ": " + products.size() + " " + t.get("products"));

        PdfPTable table = new PdfPTable(new float[]{1, 3, 2, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        addHeaderCell(table, "#");
        addHeaderCell(table, t.get("name"));
        addHeaderCell(table, t.get("sku"));
        addHeaderCell(table, t.get("stock"));
        addHeaderCell(table, t.get("price"));

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            addCell(table, String.valueOf(i + 1));
            addCell(table, p.getName());
            addCell(table, p.getSku() != null ? p.getSku() : "—");
            addBoldCell(table, String.valueOf(p.getStockQuantity()));
            addCell(table, formatPrice(p.getPrice()));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateLowStockExcel(int threshold, String lang) throws IOException {
        Map<String, String> t = getTranslations(lang);
        List<Product> products = productRepository.findByIsActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("lowStockReport"));

        CellStyle headerStyle = createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] columns = {"#", t.get("name"), t.get("sku"), t.get("stock"), t.get("price"), t.get("brand"), t.get("category")};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(p.getName());
            row.createCell(2).setCellValue(p.getSku() != null ? p.getSku() : "");
            row.createCell(3).setCellValue(p.getStockQuantity());
            row.createCell(4).setCellValue(p.getPrice().doubleValue());
            row.createCell(5).setCellValue(p.getBrand() != null ? p.getBrand() : "");
            row.createCell(6).setCellValue(p.getCategory() != null ? p.getCategory().getName() : "");
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ==================== HELPERS ====================

    private void addTitle(Document doc, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, TITLE_FONT);
        title.setSpacingAfter(5);
        doc.add(title);
    }

    private void addSubtitle(Document doc, String text) throws DocumentException {
        Paragraph sub = new Paragraph(text, SUBTITLE_FONT);
        sub.setSpacingAfter(10);
        doc.add(sub);
    }

    private void addSummary(Document doc, String text) throws DocumentException {
        Paragraph summary = new Paragraph(text, new Font(HELVETICA, 11, BOLD));
        summary.setSpacingBefore(15);
        summary.setAlignment(ALIGN_RIGHT);
        doc.add(summary);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BLACK);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addBoldCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BOLD_CELL_FONT));
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0 RSD";
        return String.format("%,.0f RSD", price);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
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

    private CellStyle createBoldStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}