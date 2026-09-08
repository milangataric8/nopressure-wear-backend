package rs.nopressurewear.util;

import rs.nopressurewear.model.User;

import java.util.Locale;

import static java.util.Objects.nonNull;

/**
 * The one place that turns "a user, maybe, and a fallback language string" into a {@link Locale}
 * for transactional emails — see {@code backend-message-i18n-spec.md} section 4.3. A known
 * {@link User} always wins (their persisted {@code preferredLocale}); {@code fallbackLang} only
 * matters for a recipient with no account row at all (a guest order's confirmation email).
 */
public final class LocaleResolver {

    private LocaleResolver() {
    }

    public static Locale of(User user, String fallbackLang) {
        String tag = nonNull(user) ? user.getPreferredLocale() : fallbackLang;
        return Locale.forLanguageTag(nonNull(tag) && !tag.isBlank() ? tag : "sr");
    }
}
