package rs.nopressurewear.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.service.email.EmailSender;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceOrderSummaryTest {

    @Mock private EmailSender emailSender;
    @Mock private StoreSettingsRepository storeSettingsRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailSender, storeSettingsRepository);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://shop.test");
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://api.test");
        lenient().when(storeSettingsRepository.findByKey(anyString())).thenReturn(Optional.empty());
    }

    private String sendAndCaptureHtml() {
        emailService.sendOrderStatusEmail(
                "customer@test.com", 42L, "AB12CD34", "SHIPPED", "Mila",
                "<table><tr><td>item</td></tr></table>",
                "3000.00",
                "Main St 1", "Novi Sad", "21000", "Serbia",
                new BigDecimal("400.00"), "sr");

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("customer@test.com"), anyString(), html.capture());
        return html.getValue();
    }

    @Test
    void orderSummary_usesTableWithRightAlignedAmounts_noFlexbox() {
        String html = sendAndCaptureHtml();

        // the summary block is a presentation table, not flexbox/float
        assertThat(html).contains("role=\"presentation\"");
        assertThat(html).doesNotContain("class=\"summary-row\"");
        assertThat(html).doesNotContain("justify-content: space-between");
        assertThat(html).doesNotContain("display: flex");

        // amounts carry both the align attribute and the CSS, and never wrap
        assertThat(html).contains("align=\"right\"");
        assertThat(html).contains("text-align:right");
        assertThat(html).contains("white-space:nowrap");
    }

    @Test
    void orderSummary_labelAndAmountAreSeparateCells_notConcatenated() {
        String html = sendAndCaptureHtml();

        // "Međuzbir" (subtotal) and its amount must be in different <td>s
        assertThat(html).doesNotContainPattern("Međuzbir\\s*2600");
        assertThat(html).containsPattern("Međuzbir</td>\\s*<td[^>]*align=\"right\"");

        // subtotal = total - delivery = 3000 - 400
        assertThat(html).contains("2600.00 RSD");
        // total row still present and bold
        assertThat(html).containsPattern("font-weight:bold[^>]*>\\s*Ukupno\\s*</td>");
        assertThat(html).containsPattern("Ukupno</td>\\s*<td[^>]*font-weight:bold[^>]*align=\"right\"|"
                + "Ukupno</td>\\s*<td[^>]*align=\"right\"[^>]*font-weight:bold");
    }

    @Test
    void orderSummary_totalIsSeparatedByBorderLineAbove() {
        String html = sendAndCaptureHtml();

        // separator is a bordered colspan cell, not <hr>
        assertThat(html).contains("colspan=\"2\"");
        assertThat(html).contains("border-top:1px solid #e5e5e5");

        int separatorIdx = html.indexOf("colspan=\"2\"");
        int totalIdx = html.indexOf("Ukupno</td>");
        assertThat(separatorIdx).isPositive();
        assertThat(totalIdx).isGreaterThan(separatorIdx); // line comes above the total row
    }

    @Test
    void deliveryRow_showsFreeLabel_whenNoDeliveryFee() {
        emailService.sendOrderStatusEmail(
                "c@test.com", 1L, "CODE", "CONFIRMED", "Ana",
                "<table></table>", "1500.00",
                "S 1", "NS", "21000", "RS",
                BigDecimal.ZERO, "sr");

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("c@test.com"), anyString(), html.capture());

        // free delivery: label, green style, no "RSD" appended, subtotal == total
        assertThat(html.getValue()).contains("Besplatno");
        assertThat(html.getValue()).contains("1500.00 RSD"); // subtotal
        assertThat(html.getValue()).containsPattern("Dostava</td>\\s*<td[^>]*align=\"right\"");
    }
}
