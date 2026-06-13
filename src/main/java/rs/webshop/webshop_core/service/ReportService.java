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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
            addCell(table, nonNull(order.getPaymentStatus()) ? order.getPaymentStatus() : "—");
            addCell(table, nonNull(order.getPaymentMethod()) ? order.getPaymentMethod() : "—");
            addCell(table, nonNull(order.getCreatedAt()) ? order.getCreatedAt().format(DATE_ONLY_FORMAT) : "—");
            totalRevenue = totalRevenue.add(nonNull(order.getTotalAmount()) ? order.getTotalAmount() : ZERO);
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
            row.createCell(4).setCellValue(nonNull(order.getTotalAmount())
                    ? order.getTotalAmount().doubleValue()
                    : 0);
            row.createCell(5).setCellValue(nonNull(order.getDiscountAmount())
                    ? order.getDiscountAmount().doubleValue()
                    : 0);
            row.createCell(6).setCellValue(nonNull(order.getCouponCode()) ? order.getCouponCode() : "");
            row.createCell(7).setCellValue(order.getStatus().name());
            row.createCell(8).setCellValue(nonNull(order.getPaymentStatus()) ? order.getPaymentStatus() : "");
            row.createCell(9).setCellValue(nonNull(order.getPaymentMethod()) ? order.getPaymentMethod() : "");
            row.createCell(10).setCellValue(nonNull(order.getShippingAddress())
                    ? order.getShippingAddress().getStreet()
                    : "");
            row.createCell(11).setCellValue(nonNull(order.getShippingAddress())
                    ? order.getShippingAddress().getCity()
                    : "");
            row.createCell(12).setCellValue(nonNull(order.getShippingAddress())
                    ? order.getShippingAddress().getPostalCode()
                    : "");
            row.createCell(13).setCellValue(nonNull(order.getShippingAddress())
                    ? order.getShippingAddress().getCountry()
                    : "");
            row.createCell(14).setCellValue(nonNull(order.getCreatedAt())
                    ? order.getCreatedAt().format(DATE_FORMAT)
                    : "");
            totalRevenue = totalRevenue.add(nonNull(order.getTotalAmount()) ? order.getTotalAmount() : ZERO);
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
            addCell(table, nonNull(p.getSku()) ? p.getSku() : "—");
            addBoldCell(table, formatPrice(p.getPrice()));
            addCell(table, nonNull(p.getDiscountPrice()) ? formatPrice(p.getDiscountPrice()) : "—");
            addCell(table, String.valueOf(p.getStockQuantity()));
            addCell(table, nonNull(p.getBrand()) ? p.getBrand() : "—");
            addCell(table, String.valueOf(nonNull(p.getSalesCount()) ? p.getSalesCount() : 0));
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
            row.createCell(2).setCellValue(nonNull(p.getSku()) ? p.getSku() : "");
            row.createCell(3).setCellValue(p.getPrice().doubleValue());
            row.createCell(4).setCellValue(nonNull(p.getDiscountPercentage())
                    ? p.getDiscountPercentage().doubleValue()
                    : 0);
            row.createCell(5).setCellValue(nonNull(p.getDiscountPrice()) ? p.getDiscountPrice().doubleValue() : 0);
            row.createCell(6).setCellValue(p.getStockQuantity());
            row.createCell(7).setCellValue(nonNull(p.getBrand()) ? p.getBrand() : "");
            row.createCell(8).setCellValue(nonNull(p.getColorName()) ? p.getColorName() : "");
            row.createCell(9).setCellValue(nonNull(p.getMaterial()) ? p.getMaterial() : "");
            row.createCell(10).setCellValue(nonNull(p.getCategory()) ? p.getCategory().getName() : "");
            row.createCell(11).setCellValue(nonNull(p.getSalesCount()) ? p.getSalesCount() : 0);
            row.createCell(12).setCellValue(nonNull(p.getAverageRating()) ? p.getAverageRating().doubleValue() : 0);
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
            addCell(table, nonNull(p.getSku()) ? p.getSku() : "—");
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
        String[] columns = {
                "#",
                t.get("name"),
                t.get("sku"),
                t.get("stock"),
                t.get("price"),
                t.get("brand"),
                t.get("category")
        };

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
            row.createCell(2).setCellValue(nonNull(p.getSku()) ? p.getSku() : "");
            row.createCell(3).setCellValue(p.getStockQuantity());
            row.createCell(4).setCellValue(p.getPrice().doubleValue());
            row.createCell(5).setCellValue(nonNull(p.getBrand()) ? p.getBrand() : "");
            row.createCell(6).setCellValue(nonNull(p.getCategory()) ? p.getCategory().getName() : "");
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

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

        addTotalRow(totalsTable, t.get("delivery"), t.get("free"));

        PdfPCell totalLabel = new PdfPCell(new Phrase(t.get("totalLabel"), new Font(HELVETICA, 11, BOLD)));
        totalLabel.setBorderWidth(0);
        totalLabel.setBorderWidthTop(1);
        totalLabel.setPaddingTop(8);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
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