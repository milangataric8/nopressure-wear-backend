package rs.nopressurewear.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whitespace normalization for rich-text HTML saved through the admin WYSIWYG editors.
 *
 * <p>{@code contenteditable} editors emit {@code &nbsp;} on <em>every</em> space between
 * words. Because a non-breaking space forbids a line break, the browser then treats a
 * whole paragraph as one unbreakable "word" and is forced to break <em>inside</em> a
 * word. This class removes that noise (Phase A) and re-applies non-breaking spaces only
 * where typography genuinely needs them - numbers with units, abbreviations, initials,
 * one-letter prepositions (Phase B).
 *
 * <p>This is <strong>not</strong> an XSS sanitizer - see {@link HtmlSanitizer} for that.
 * It only touches whitespace, and only inside text content: tag markup and attribute
 * values are left byte-for-byte untouched (the input is split on {@code <[^>]*>} and only
 * the text segments are processed).
 *
 * <p>Idempotent: Phase B emits the {@code &nbsp;} entity and Phase A converts that entity
 * back to a plain space, so {@code normalizeWhitespace(normalizeWhitespace(x))} equals
 * {@code normalizeWhitespace(x)}.
 */
public final class HtmlTextSanitizer {

    private HtmlTextSanitizer() {
    }

    private static final String NBSP = "&nbsp;";
    private static final char NO_BREAK_SPACE = ' ';

    /** Whole tags: everything from a '<' to the next '>'. Tags are copied through verbatim. */
    private static final Pattern TAG = Pattern.compile("<[^>]*>");

    // ---- Phase A: strip ----
    private static final Pattern NBSP_ENTITY =
            Pattern.compile("&nbsp;|&#0*160;|&#x0*a0;", Pattern.CASE_INSENSITIVE);
    /** Zero-width space / non-joiner / joiner, BOM, and soft hyphen. */
    private static final Pattern ZERO_WIDTH =
            Pattern.compile("[​‌‍﻿­]");
    private static final Pattern MULTI_SPACE =
            Pattern.compile("[ \t]{2,}");

    // ---- Phase B: re-apply ----
    // Units ordered longest / most specific first so the alternation picks the right one
    // (e.g. "kg" before "g", "mm" before "m"). The trailing lookahead stops "5 meters"
    // from matching "m".
    private static final Pattern B1_NUMBER_UNIT = Pattern.compile(
            "(\\d) (RSD|EUR|USD|BAM|din|min|kg|mg|km|cm|mm|ml|g|t|m|l|h|s|€|\\$|£)(?![\\p{L}\\d])");
    private static final Pattern B2_NUMBER_PERCENT = Pattern.compile("(\\d) (%)");
    private static final Pattern B3_ABBR_NUMBER = Pattern.compile(
            "(?<!\\p{L})(str|br|tel|tj|tzv|god|čl|tač)\\. (\\d)");
    private static final Pattern B4_INITIAL_NAME = Pattern.compile(
            "(?<!\\p{L})(\\p{Lu})\\. (\\p{Lu})");
    // The following token must be a real word (2+ letters), so "a b" / initials are left alone.
    private static final Pattern B5_ONE_LETTER = Pattern.compile(
            "(?<!\\p{L})([aiouskAIOUSK]) (\\p{L}{2,})");

    /**
     * Normalizes whitespace in an HTML fragment. {@code null} or blank input is returned
     * unchanged, without throwing.
     */
    public static String normalizeWhitespace(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        Matcher m = TAG.matcher(html);
        StringBuilder out = new StringBuilder(html.length() + 16);
        int last = 0;
        while (m.find()) {
            out.append(processText(html, last, m.start()));
            out.append(m.group());
            last = m.end();
        }
        out.append(processText(html, last, html.length()));

        // Phase A also trims the leading/trailing whitespace of the whole string.
        return out.toString().strip();
    }

    private static String processText(String src, int from, int to) {
        if (from >= to) {
            return "";
        }
        return reapply(strip(src.substring(from, to)));
    }

    private static String strip(String s) {
        s = NBSP_ENTITY.matcher(s).replaceAll(" ");
        s = s.replace(NO_BREAK_SPACE, ' ');
        s = ZERO_WIDTH.matcher(s).replaceAll("");
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        return s;
    }

    private static String reapply(String s) {
        s = B1_NUMBER_UNIT.matcher(s).replaceAll("$1" + NBSP + "$2");
        s = B2_NUMBER_PERCENT.matcher(s).replaceAll("$1" + NBSP + "$2");
        s = B3_ABBR_NUMBER.matcher(s).replaceAll("$1." + NBSP + "$2");
        s = B4_INITIAL_NAME.matcher(s).replaceAll("$1." + NBSP + "$2");
        s = B5_ONE_LETTER.matcher(s).replaceAll("$1" + NBSP + "$2");
        return s;
    }
}
