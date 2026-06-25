package rs.nopressurewear.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("s", "u")
            .preserveRelativeLinks(false);

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, SAFELIST);
    }
}
