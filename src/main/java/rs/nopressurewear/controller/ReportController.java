package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.service.report.CustomerReportService;
import rs.nopressurewear.service.report.OrderReportService;
import rs.nopressurewear.service.report.ProductReportService;
import rs.nopressurewear.service.report.RevenueReportService;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.parseMediaType;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final OrderReportService orderReportService;
    private final ProductReportService productReportService;
    private final CustomerReportService customerReportService;
    private final RevenueReportService revenueReportService;

    @GetMapping("/orders/pdf")
    public ResponseEntity<byte[]> ordersPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(orderReportService.generateOrdersPdf(lang), "orders-report.pdf");
    }

    @GetMapping("/orders/excel")
    public ResponseEntity<byte[]> ordersExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(orderReportService.generateOrdersExcel(lang), "orders-report.xlsx");
    }

    @GetMapping("/products/pdf")
    public ResponseEntity<byte[]> productsPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(productReportService.generateProductsPdf(lang), "products-report.pdf");
    }

    @GetMapping("/products/excel")
    public ResponseEntity<byte[]> productsExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(productReportService.generateProductsExcel(lang), "products-report.xlsx");
    }

    @GetMapping("/customers/pdf")
    public ResponseEntity<byte[]> customersPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(customerReportService.generateCustomersPdf(lang), "customers-report.pdf");
    }

    @GetMapping("/customers/excel")
    public ResponseEntity<byte[]> customersExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(customerReportService.generateCustomersExcel(lang), "customers-report.xlsx");
    }

    @GetMapping("/low-stock/pdf")
    public ResponseEntity<byte[]> lowStockPdf(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(productReportService.generateLowStockPdf(threshold, lang), "low-stock-report.pdf");
    }

    @GetMapping("/low-stock/excel")
    public ResponseEntity<byte[]> lowStockExcel(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(productReportService.generateLowStockExcel(threshold, lang), "low-stock-report.xlsx");
    }

    @GetMapping("/revenue-by-category/pdf")
    public ResponseEntity<byte[]> revenueByCategoryPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(revenueReportService.generateRevenueByCategoryPdf(lang), "revenue-by-category-report.pdf");
    }

    @GetMapping("/revenue-by-category/excel")
    public ResponseEntity<byte[]> revenueByCategoryExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(revenueReportService.generateRevenueByCategoryExcel(lang), "revenue-by-category-report.xlsx");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(APPLICATION_PDF)
                .body(data);
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
