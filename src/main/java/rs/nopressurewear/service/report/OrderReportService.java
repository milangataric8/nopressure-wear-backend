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
import rs.nopressurewear.model.Order;
import rs.nopressurewear.repository.OrderRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.lowagie.text.PageSize.A4;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class OrderReportService {

    private final OrderRepository orderRepository;
    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public byte[] generateOrdersPdf(String lang) throws DocumentException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        reportService.addTitle(document, t.get("ordersReport"));
        reportService.addSubtitle(document, t.get("generated") + ": " + LocalDateTime.now().format(DATE_FORMAT)
                + " | " + t.get("total") + ": " + orders.size() + " " + t.get("orders"));

        PdfPTable table = new PdfPTable(new float[]{1, 2, 3, 2, 2, 2, 2, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);

        reportService.addHeaderCell(table, "#");
        reportService.addHeaderCell(table, t.get("orderCode"));
        reportService.addHeaderCell(table, t.get("customer"));
        reportService.addHeaderCell(table, t.get("amount"));
        reportService.addHeaderCell(table, t.get("status"));
        reportService.addHeaderCell(table, t.get("paymentStatus"));
        reportService.addHeaderCell(table, t.get("paymentMethod"));
        reportService.addHeaderCell(table, t.get("date"));

        BigDecimal totalRevenue = ZERO;
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            reportService.addCell(table, String.valueOf(i + 1));
            reportService.addCell(table, order.getOrderCode());
            reportService.addCell(table, order.getCustomerFullName());
            reportService.addBoldCell(table, reportService.formatPrice(order.getTotalAmount()));
            reportService.addCell(table, order.getStatus().name());
            reportService.addCell(table, nonNull(order.getPaymentStatus()) ? order.getPaymentStatus() : "—");
            reportService.addCell(table, nonNull(order.getPaymentMethod()) ? order.getPaymentMethod() : "—");
            reportService.addCell(table, nonNull(order.getCreatedAt()) ? order.getCreatedAt().format(DATE_ONLY_FORMAT) : "—");
            totalRevenue = totalRevenue.add(nonNull(order.getTotalAmount()) ? order.getTotalAmount() : ZERO);
        }

        document.add(table);
        reportService.addSummary(document, t.get("totalRevenue") + ": " + reportService.formatPrice(totalRevenue));
        document.close();
        return out.toByteArray();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public byte[] generateOrdersExcel(String lang) throws IOException {
        Map<String, String> t = reportService.getTranslations(lang);
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(t.get("ordersReport"));

        CellStyle headerStyle = reportService.createHeaderStyle(workbook);
        CellStyle boldStyle = reportService.createBoldStyle(workbook);

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
            row.createCell(4).setCellValue(nonNull(order.getTotalAmount()) ? order.getTotalAmount().doubleValue() : 0);
            row.createCell(5).setCellValue(nonNull(order.getDiscountAmount()) ? order.getDiscountAmount().doubleValue() : 0);
            row.createCell(6).setCellValue(nonNull(order.getCouponCode()) ? order.getCouponCode() : "");
            row.createCell(7).setCellValue(order.getStatus().name());
            row.createCell(8).setCellValue(nonNull(order.getPaymentStatus()) ? order.getPaymentStatus() : "");
            row.createCell(9).setCellValue(nonNull(order.getPaymentMethod()) ? order.getPaymentMethod() : "");
            row.createCell(10).setCellValue(nonNull(order.getShippingAddress()) ? order.getShippingAddress().getStreet() : "");
            row.createCell(11).setCellValue(nonNull(order.getShippingAddress()) ? order.getShippingAddress().getCity() : "");
            row.createCell(12).setCellValue(nonNull(order.getShippingAddress()) ? order.getShippingAddress().getPostalCode() : "");
            row.createCell(13).setCellValue(nonNull(order.getShippingAddress()) ? order.getShippingAddress().getCountry() : "");
            row.createCell(14).setCellValue(nonNull(order.getCreatedAt()) ? order.getCreatedAt().format(DATE_FORMAT) : "");
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
}
