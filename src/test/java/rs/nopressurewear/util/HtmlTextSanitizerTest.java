package rs.nopressurewear.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static rs.nopressurewear.util.HtmlTextSanitizer.normalizeWhitespace;

class HtmlTextSanitizerTest {

    private static final String NBSP_CHAR = " ";
    private static final String ZERO_WIDTH_SPACE = "​";
    private static final String SOFT_HYPHEN = "­";
    private static final String EURO = "€";
    private static final String C_CARON = "č"; // č

    @Test
    void nbspBetweenOrdinaryWords_becomesPlainSpace() {
        String in = "izaberes&nbsp;svedenu&nbsp;belu&nbsp;polo&nbsp;majicu";
        assertThat(normalizeWhitespace(in)).isEqualTo("izaberes svedenu belu polo majicu");
    }

    @Test
    void literalNbspCharAndNumericEntities_becomePlainSpace() {
        assertThat(normalizeWhitespace("a" + NBSP_CHAR + "b")).isEqualTo("a b");
        assertThat(normalizeWhitespace("a&#160;b")).isEqualTo("a b");
        assertThat(normalizeWhitespace("a&#xA0;b")).isEqualTo("a b");
        assertThat(normalizeWhitespace("a&NBSP;b")).isEqualTo("a b");
    }

    @Test
    void zeroWidthSpaceAndSoftHyphen_areRemoved() {
        assertThat(normalizeWhitespace("wo" + ZERO_WIDTH_SPACE + "rd")).isEqualTo("word");
        assertThat(normalizeWhitespace("wo" + SOFT_HYPHEN + "rd")).isEqualTo("word");
    }

    @Test
    void multipleConsecutiveSpaces_collapseToOne() {
        assertThat(normalizeWhitespace("foo    bar\t\tbaz")).isEqualTo("foo bar baz");
    }

    @Test
    void leadingAndTrailingWhitespace_isTrimmed() {
        assertThat(normalizeWhitespace("   hello world   ")).isEqualTo("hello world");
    }

    @Test
    void phaseB_reappliesNbspWhereTypographyNeedsIt() {
        assertThat(normalizeWhitespace("50 " + EURO)).isEqualTo("50&nbsp;" + EURO);
        assertThat(normalizeWhitespace("10 kg")).isEqualTo("10&nbsp;kg");
        assertThat(normalizeWhitespace("20 %")).isEqualTo("20&nbsp;%");
        assertThat(normalizeWhitespace("str. 12")).isEqualTo("str.&nbsp;12");
        assertThat(normalizeWhitespace("M. Gatari" + C_CARON)).isEqualTo("M.&nbsp;Gatari" + C_CARON);
        assertThat(normalizeWhitespace("u Novom Sadu")).isEqualTo("u&nbsp;Novom Sadu");
    }

    @Test
    void phaseB_doesNotTouchWordsThatMerelyStartWithAOneLetterToken() {
        assertThat(normalizeWhitespace("kilogram paprike")).isEqualTo("kilogram paprike");
        assertThat(normalizeWhitespace("stati" + C_CARON + "ka analiza")).isEqualTo("stati" + C_CARON + "ka analiza");
    }

    @Test
    void tagMarkupAndAttributes_areLeftUntouched() {
        String in = "<a href=\"a b\">u Novom</a> i <span style=\"margin:  0\">50 " + EURO + "</span>";
        String out = normalizeWhitespace(in);
        assertThat(out).contains("href=\"a b\"");
        assertThat(out).contains("style=\"margin:  0\"");
        assertThat(out).contains(">u&nbsp;Novom<");
        assertThat(out).contains(">50&nbsp;" + EURO + "<");
    }

    @Test
    void operationIsIdempotent() {
        String in = "  Cena je 50 " + EURO + " za 2 kg, vidi str. 12, pise M. Gatari" + C_CARON
                + " u Novom Sadu.  text&nbsp;with&nbsp;nbsp" + ZERO_WIDTH_SPACE;
        String once = normalizeWhitespace(in);
        String twice = normalizeWhitespace(once);
        assertThat(twice).isEqualTo(once);
    }

    @Test
    void nullAndBlankInput_areReturnedAsIsWithoutThrowing() {
        assertThat(normalizeWhitespace(null)).isNull();
        assertThat(normalizeWhitespace("")).isEqualTo("");
        assertThat(normalizeWhitespace("   ")).isEqualTo("   ");
    }
}
