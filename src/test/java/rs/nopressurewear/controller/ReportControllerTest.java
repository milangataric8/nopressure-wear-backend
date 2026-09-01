package rs.nopressurewear.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import rs.nopressurewear.service.report.CustomerReportService;
import rs.nopressurewear.service.report.OrderReportService;
import rs.nopressurewear.service.report.ProductReportService;
import rs.nopressurewear.service.report.RevenueReportService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevenueReportService revenueReportService;

    @MockitoBean
    private OrderReportService orderReportService;

    @MockitoBean
    private ProductReportService productReportService;

    @MockitoBean
    private CustomerReportService customerReportService;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void revenueByCategoryPdf_ShouldReturn200AndPdf_ForAdmin() throws Exception {
        when(revenueReportService.generateRevenueByCategoryPdf(anyString())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reports/revenue-by-category/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void revenueByCategoryExcel_ShouldReturn200AndXlsx_ForAdmin() throws Exception {
        when(revenueReportService.generateRevenueByCategoryExcel(anyString())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reports/revenue-by-category/excel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void revenueByCategoryPdf_ShouldReturn403_ForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/reports/revenue-by-category/pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void revenueByCategoryExcel_ShouldReturn403_ForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/reports/revenue-by-category/excel"))
                .andExpect(status().isForbidden());
    }
}
