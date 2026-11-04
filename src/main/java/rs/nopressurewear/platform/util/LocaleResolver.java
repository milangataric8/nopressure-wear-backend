package rs.nopressurewear.platform.util;

import rs.nopressurewear.platform.api.LocalizedRecipient;

import java.util.Locale;

import static java.util.Objects.nonNull;

/**
 * The one place that turns "a user, maybe, and a fallback language string" into a {@link Locale}
 * for transactional emails — see {@code backend-message-i18n-spec.md} section 4.3. A known
 * {@link LocalizedRecipient} always wins (their persisted {@code preferredLocale}); {@code fallbackLang} only
 * matters for a recipient with no account row at all (a guest order's confirmation email).
 */
public final class LocaleResolver {

    private LocaleResolver() {
    }

    public static Locale of(LocalizedRecipient user, String fallbackLang) {
        String tag = nonNull(user) ? user.getPreferredLocale() : fallbackLang;
        return normalize(tag);
    }

    /** Only supported languages are accepted; regional tags share the base translation. */
    public static Locale normalize(String tag) {
        String language = tag == null ? "" : Locale.forLanguageTag(tag.trim()).getLanguage();
        return Locale.forLanguageTag("en".equals(language) ? "en" : "sr");
    }
}
