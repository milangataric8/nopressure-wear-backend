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
import rs.nopressurewear.repository.OrderRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.lowagie.text.PageSize.A4;

@Service
@RequiredArgsConstructor
public class CustomerReportService {

    private final OrderRepository orderRepository;
    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateCustomersPdf(String lang) throws DocumentException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Map<String, Object>> customers = orderRepository.findAllCustomerStats();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        reportService.addTitle(document, t.get("customersReport"));
        reportService.addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + customers.size() + " " + t.get("customers"));

        PdfPTable table = new PdfPTable(new float[]{1, 3, 3, 1.5f, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        reportService.addHeaderCell(table, "#");
        reportService.addHeaderCell(table, t.get("name"));
        reportService.addHeaderCell(table, t.get("email"));
        reportService.addHeaderCell(table, t.get("orders"));
        reportService.addHeaderCell(table, t.get("totalSpent"));

        for (int i = 0; i < customers.size(); i++) {
            Map<String, Object> c = customers.get(i);
            reportService.addCell(table, String.valueOf(i + 1));
            reportService.addCell(table, String.valueOf(c.get("name")));
            reportService.addCell(table, String.valueOf(c.get("email")));
            reportService.addCell(table, String.valueOf(c.get("orders")));
            reportService.addBoldCell(table, reportService.formatPrice((BigDecimal) c.get("total_spent")));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public byte[] generateCustomersExcel(String lang) throws IOException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Map<String, Object>> customers = orderRepository.findAllCustomerStats();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("customersReport"));

        CellStyle headerStyle = reportService.createHeaderStyle(workbook);

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
}
