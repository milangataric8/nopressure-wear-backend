package rs.nopressurewear.service.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.ProductVariantRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lowagie.text.PageSize.A4;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class ProductReportService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Total variant stock per product id, in one query (no per-row lookup). Missing product => 0. */
    private Map<Long, Integer> stockByProduct(List<Product> products) {
        Map<Long, Integer> map = new HashMap<>();
        if (products.isEmpty()) return map;
        List<Long> ids = products.stream().map(Product::getId).toList();
        for (Object[] row : productVariantRepository.sumStockByProductIds(ids)) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return map;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateProductsPdf(String lang) throws DocumentException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Product> products = productRepository.findAll();
        Map<Long, Integer> stock = stockByProduct(products);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        reportService.addTitle(document, t.get("productsReport"));
        reportService.addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + products.size() + " " + t.get("products"));

        PdfPTable table = new PdfPTable(new float[]{1, 3, 2, 1.5f, 1.5f, 1, 1.5f, 1, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        reportService.addHeaderCell(table, "#");
        reportService.addHeaderCell(table, t.get("name"));
        reportService.addHeaderCell(table, t.get("sku"));
        reportService.addHeaderCell(table, t.get("price"));
        reportService.addHeaderCell(table, t.get("discountPrice"));
        reportService.addHeaderCell(table, t.get("stock"));
        reportService.addHeaderCell(table, t.get("brand"));
        reportService.addHeaderCell(table, t.get("salesCount"));
        reportService.addHeaderCell(table, t.get("status"));

        BigDecimal totalValue = ZERO;
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int qty = stock.getOrDefault(p.getId(), 0);
            reportService.addCell(table, String.valueOf(i + 1));
            reportService.addCell(table, p.getName());
            reportService.addCell(table, nonNull(p.getSku()) ? p.getSku() : "—");
            reportService.addBoldCell(table, reportService.formatPrice(p.getPrice()));
            reportService.addCell(table, nonNull(p.getDiscountPrice()) ? reportService.formatPrice(p.getDiscountPrice()) : "—");
            reportService.addCell(table, String.valueOf(qty));
            reportService.addCell(table, nonNull(p.getBrand()) ? p.getBrand() : "—");
            reportService.addCell(table, String.valueOf(nonNull(p.getSalesCount()) ? p.getSalesCount() : 0));
            reportService.addCell(table, p.isActive() ? t.get("active") : t.get("inactive"));
            totalValue = totalValue.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        document.add(table);
        reportService.addSummary(document, t.get("totalStockValue") + ": " + reportService.formatPrice(totalValue));
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateProductsExcel(String lang) throws IOException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Product> products = productRepository.findAll();
        Map<Long, Integer> stock = stockByProduct(products);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("productsReport"));

        CellStyle headerStyle = reportService.createHeaderStyle(workbook);
        CellStyle boldStyle = reportService.createBoldStyle(workbook);

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
            int qty = stock.getOrDefault(p.getId(), 0);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(p.getName());
            row.createCell(2).setCellValue(nonNull(p.getSku()) ? p.getSku() : "");
            row.createCell(3).setCellValue(p.getPrice().doubleValue());
            row.createCell(4).setCellValue(nonNull(p.getDiscountPercentage()) ? p.getDiscountPercentage().doubleValue() : 0);
            row.createCell(5).setCellValue(nonNull(p.getDiscountPrice()) ? p.getDiscountPrice().doubleValue() : 0);
            row.createCell(6).setCellValue(qty);
            row.createCell(7).setCellValue(nonNull(p.getBrand()) ? p.getBrand() : "");
            row.createCell(8).setCellValue(nonNull(p.getColorName()) ? p.getColorName() : "");
            row.createCell(9).setCellValue(nonNull(p.getMaterial()) ? p.getMaterial() : "");
            row.createCell(10).setCellValue(nonNull(p.getCategory()) ? p.getCategory().getName() : "");
            row.createCell(11).setCellValue(nonNull(p.getSalesCount()) ? p.getSalesCount() : 0);
            row.createCell(12).setCellValue(nonNull(p.getAverageRating()) ? p.getAverageRating().doubleValue() : 0);
            row.createCell(13).setCellValue(p.isActive() ? t.get("active") : t.get("inactive"));
            totalValue = totalValue.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
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
    public byte[] generateLowStockPdf(int threshold, String lang) throws DocumentException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Map<String, Object>> rows = productRepository.findLowStockVariants(threshold);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        reportService.addTitle(document, t.get("lowStockReport"));
        reportService.addSubtitle(document, t.get("threshold") + ": " + threshold + " " + t.get("units")
                + " | " + t.get("total") + ": " + rows.size());

        // Column order: Product, Size, Stock
        PdfPTable table = new PdfPTable(new float[]{1, 4, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        reportService.addHeaderCell(table, "#");
        reportService.addHeaderCell(table, t.get("name"));
        reportService.addHeaderCell(table, t.get("size"));
        reportService.addHeaderCell(table, t.get("stock"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            reportService.addCell(table, String.valueOf(i + 1));
            reportService.addCell(table, String.valueOf(r.get("name")));
            reportService.addCell(table, String.valueOf(r.get("size")));
            reportService.addBoldCell(table, String.valueOf(r.get("stockQuantity")));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateLowStockExcel(int threshold, String lang) throws IOException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Map<String, Object>> rows = productRepository.findLowStockVariants(threshold);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("lowStockReport"));

        CellStyle headerStyle = reportService.createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] columns = {"#", t.get("name"), t.get("size"), t.get("stock")};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(String.valueOf(r.get("name")));
            row.createCell(2).setCellValue(String.valueOf(r.get("size")));
            row.createCell(3).setCellValue(((Number) r.get("stockQuantity")).doubleValue());
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }
}
