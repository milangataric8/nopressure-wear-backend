package rs.nopressurewear.service.report;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.lowagie.text.Element.ALIGN_RIGHT;
import static com.lowagie.text.Font.HELVETICA;
import static java.awt.Color.*;
import static java.awt.Font.BOLD;
import static java.awt.Frame.NORMAL;
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
            t.put("revenueByCategoryReport", "Izveštaj prihoda po kategoriji");
            t.put("generated", "Generisano");
            t.put("total", "Ukupno");
            t.put("revenue", "Prihod");
            t.put("sharePercent", "Udeo %");
            t.put("categories", "kategorija");

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
            t.put("revenueByCategoryReport", "Revenue by Category Report");
            t.put("generated", "Generated");
            t.put("total", "Total");
            t.put("revenue", "Revenue");
            t.put("sharePercent", "Share %");
            t.put("categories", "categories");

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

}
