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
import rs.nopressurewear.service.DashboardService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.lowagie.text.PageSize.A4;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.nonNull;

/**
 * PDF / Excel export for the dashboard's "Revenue by Category" breakdown. Mirrors the
 * other four report services; the figures come from {@link DashboardService#getRevenueByCategory()}
 * so the export and the on-screen chart can never disagree.
 */
@Service
@RequiredArgsConstructor
public class RevenueReportService {

    private final DashboardService dashboardService;
    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateRevenueByCategoryPdf(String lang) throws DocumentException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Map<String, Object>> rows = sortedByRevenueDesc(dashboardService.getRevenueByCategory());
        BigDecimal totalRevenue = totalRevenue(rows);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        reportService.addTitle(document, t.get("revenueByCategoryReport"));
        reportService.addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + rows.size() + " " + t.get("categories"));

        PdfPTable table = new PdfPTable(new float[]{1, 4, 3, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        reportService.addHeaderCell(table, "#");
        reportService.addHeaderCell(table, t.get("category"));
        reportService.addHeaderCell(table, t.get("revenue"));
        reportService.addHeaderCell(table, t.get("sharePercent"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            BigDecimal revenue = revenueOf(row);
            reportService.addCell(table, String.valueOf(i + 1));
            reportService.addCell(table, categoryOf(row));
            reportService.addBoldCell(table, reportService.formatPrice(revenue));
            reportService.addCell(table, formatShare(share(revenue, totalRevenue)));
        }

        document.add(table);

        if (!rows.isEmpty()) {
            reportService.addSummary(document, t.get("totalRevenue") + ": " + reportService.formatPrice(totalRevenue));
        }

        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateRevenueByCategoryExcel(String lang) throws IOException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Map<String, Object>> rows = sortedByRevenueDesc(dashboardService.getRevenueByCategory());
        BigDecimal totalRevenue = totalRevenue(rows);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("revenueByCategoryReport"));

        CellStyle headerStyle = reportService.createHeaderStyle(workbook);
        CellStyle boldStyle = reportService.createBoldStyle(workbook);

        Row header = sheet.createRow(0);
        String[] columns = {"#", t.get("category"), t.get("revenue"), t.get("sharePercent")};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            BigDecimal revenue = revenueOf(row);
            Row excelRow = sheet.createRow(i + 1);
            excelRow.createCell(0).setCellValue(i + 1);
            excelRow.createCell(1).setCellValue(categoryOf(row));
            excelRow.createCell(2).setCellValue(revenue.doubleValue());
            excelRow.createCell(3).setCellValue(share(revenue, totalRevenue).doubleValue());
        }

        if (!rows.isEmpty()) {
            Row summaryRow = sheet.createRow(rows.size() + 2);
            Cell label = summaryRow.createCell(1);
            label.setCellValue(t.get("totalRevenue") + ":");
            label.setCellStyle(boldStyle);
            Cell value = summaryRow.createCell(2);
            value.setCellValue(totalRevenue.doubleValue());
            value.setCellStyle(boldStyle);
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private static List<Map<String, Object>> sortedByRevenueDesc(List<Map<String, Object>> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(RevenueReportService::revenueOf).reversed())
                .toList();
    }

    private static BigDecimal totalRevenue(List<Map<String, Object>> rows) {
        return rows.stream().map(RevenueReportService::revenueOf).reduce(ZERO, BigDecimal::add);
    }

    /** This row's revenue as a percentage of the total, one decimal. Zero total -> 0. */
    private static BigDecimal share(BigDecimal revenue, BigDecimal total) {
        if (total == null || total.compareTo(ZERO) == 0) {
            return ZERO.setScale(1);
        }
        return revenue.multiply(BigDecimal.valueOf(100)).divide(total, 1, HALF_UP);
    }

    private static String formatShare(BigDecimal share) {
        return share.toPlainString() + " %";
    }

    private static BigDecimal revenueOf(Map<String, Object> row) {
        Object value = row.get("revenue");
        if (value == null) return ZERO;
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }

    private static String categoryOf(Map<String, Object> row) {
        Object value = row.get("category");
        return nonNull(value) ? value.toString() : "—";
    }
}
