package rs.webshop.webshop_core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.service.ReportService;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.parseMediaType;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/orders/pdf")
    public ResponseEntity<byte[]> ordersPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(reportService.generateOrdersPdf(lang), "orders-report.pdf");
    }

    @GetMapping("/orders/excel")
    public ResponseEntity<byte[]> ordersExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(reportService.generateOrdersExcel(lang), "orders-report.xlsx");
    }

    @GetMapping("/products/pdf")
    public ResponseEntity<byte[]> productsPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(reportService.generateProductsPdf(lang), "products-report.pdf");
    }

    @GetMapping("/products/excel")
    public ResponseEntity<byte[]> productsExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(reportService.generateProductsExcel(lang), "products-report.xlsx");
    }

    @GetMapping("/customers/pdf")
    public ResponseEntity<byte[]> customersPdf(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(reportService.generateCustomersPdf(lang), "customers-report.pdf");
    }

    @GetMapping("/customers/excel")
    public ResponseEntity<byte[]> customersExcel(@RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(reportService.generateCustomersExcel(lang), "customers-report.xlsx");
    }

    @GetMapping("/low-stock/pdf")
    public ResponseEntity<byte[]> lowStockPdf(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildPdfResponse(reportService.generateLowStockPdf(threshold, lang), "low-stock-report.pdf");
    }

    @GetMapping("/low-stock/excel")
    public ResponseEntity<byte[]> lowStockExcel(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "en") String lang) throws Exception {
        return buildExcelResponse(reportService.generateLowStockExcel(threshold, lang), "low-stock-report.xlsx");
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