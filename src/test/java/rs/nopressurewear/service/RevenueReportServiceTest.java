package rs.nopressurewear.service;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rs.nopressurewear.service.report.ReportService;
import rs.nopressurewear.service.report.RevenueReportService;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueReportServiceTest {

    @Mock
    private DashboardService dashboardService;

    private RevenueReportService revenueReportService;

    @BeforeEach
    void setUp() {
        revenueReportService = new RevenueReportService(dashboardService, new ReportService());
    }

    /** Top-level category row: no parent. Keys mirror the lowercased native-query aliases. */
    private static Map<String, Object> row(String category, String revenue) {
        return row(null, category, revenue);
    }

    /** Subcategory row under {@code parentName} (null = top level). */
    private static Map<String, Object> row(String parentName, String category, String revenue) {
        Map<String, Object> m = new HashMap<>();
        m.put("categoryid", (long) category.hashCode());
        m.put("categoryname", category);
        m.put("parentid", parentName == null ? null : (long) parentName.hashCode());
        m.put("parentname", parentName);
        m.put("revenue", new BigDecimal(revenue));
        return m;
    }

    @Test
    void nonEmptyDataset_producesNonEmptyPdfAndExcel() throws Exception {
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of(
                row("Shoes", "1000"), row("Shirts", "500"), row("Hats", "250")));

        byte[] pdf = revenueReportService.generateRevenueByCategoryPdf("en");
        byte[] excel = revenueReportService.generateRevenueByCategoryExcel("en");

        assertThat(pdf).isNotEmpty();
        assertThat(excel).isNotEmpty();
        assertThat(new PdfReader(pdf).getNumberOfPages()).isPositive();
    }

    @Test
    void emptyDataset_producesValidFileWithHeadersOnly() throws Exception {
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of());

        byte[] pdf = revenueReportService.generateRevenueByCategoryPdf("en");
        byte[] excel = revenueReportService.generateRevenueByCategoryExcel("en");

        assertThat(pdf).isNotEmpty();
        assertThat(new PdfReader(pdf).getNumberOfPages()).isPositive();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isZero(); // header row only, no data, no total row
            Row header = sheet.getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) 5);
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Parent Category");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("Category");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Revenue");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Share %");
        }
    }

    @Test
    void sharePercentagesSumTo100() throws Exception {
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of(
                row("Shoes", "1234.56"), row("Shirts", "555.44"), row("Hats", "210.00")));

        byte[] excel = revenueReportService.generateRevenueByCategoryExcel("en");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            double sum = 0;
            for (int r = 1; r <= 3; r++) {
                sum += sheet.getRow(r).getCell(4).getNumericCellValue();
            }
            assertThat(sum).isCloseTo(100.0, within(0.5));
        }
    }

    @Test
    void rowsAreOrderedByRevenueDescending_evenWhenSourceIsNot() throws Exception {
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of(
                row("Hats", "250"), row("Shoes", "1000"), row("Shirts", "500")));

        byte[] excel = revenueReportService.generateRevenueByCategoryExcel("en");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Shoes");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Shirts");
            assertThat(sheet.getRow(3).getCell(2).getStringCellValue()).isEqualTo("Hats");
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue())
                    .isGreaterThanOrEqualTo(sheet.getRow(2).getCell(3).getNumericCellValue());
            assertThat(sheet.getRow(2).getCell(3).getNumericCellValue())
                    .isGreaterThanOrEqualTo(sheet.getRow(3).getCell(3).getNumericCellValue());
        }
    }

    @Test
    void topLevelCategory_hasBlankParentColumn_subcategoryShowsParentName() throws Exception {
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of(
                row("Apparel", "1000"),                 // top level, no parent
                row("Apparel", "T-Shirts", "600"),      // subcategory of Apparel
                row("Footwear", "Sneakers", "400")));   // subcategory of Footwear

        byte[] excel = revenueReportService.generateRevenueByCategoryExcel("en");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            // row 1 -> Apparel (1000), row 2 -> T-Shirts (600), row 3 -> Sneakers (400)
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEmpty();      // parent blank, not "Apparel"
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Apparel");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Apparel");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("T-Shirts");
            assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Footwear");
            assertThat(sheet.getRow(3).getCell(2).getStringCellValue()).isEqualTo("Sneakers");
        }
    }

    @Test
    void sameSubcategoryNameUnderDifferentParents_isDistinguishableByParent() throws Exception {
        // "Accessories" exists under two parents; only the parent column tells the rows apart.
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of(
                row("Men", "Accessories", "700"),
                row("Women", "Accessories", "300")));

        byte[] excel = revenueReportService.generateRevenueByCategoryExcel("en");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Men");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Accessories");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Women");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Accessories");
        }
    }

    @Test
    void pdfExport_isGeneratedWithHierarchyRows() throws Exception {
        when(dashboardService.getRevenueByCategory()).thenReturn(List.of(
                row("Apparel", "1000"),
                row("Apparel", "T-Shirts", "600")));

        byte[] pdf = revenueReportService.generateRevenueByCategoryPdf("en");

        assertThat(pdf).isNotEmpty();
        assertThat(new PdfReader(pdf).getNumberOfPages()).isPositive();
    }
}
