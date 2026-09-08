package rs.nopressurewear.util;

import org.junit.jupiter.api.Test;
import rs.nopressurewear.model.User;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** backend-message-i18n-spec.md section 4.3: a known recipient's own preference always wins. */
class LocaleResolverTest {

    @Test
    void aKnownUser_usesTheirOwnPreferredLocale_regardlessOfFallback() {
        User user = User.builder().preferredLocale("en").build();

        assertThat(LocaleResolver.of(user, "sr")).isEqualTo(Locale.forLanguageTag("en"));
    }

    @Test
    void noUser_fallsBackToTheGivenLanguage() {
        assertThat(LocaleResolver.of(null, "en")).isEqualTo(Locale.forLanguageTag("en"));
    }

    @Test
    void noUserAndNoFallback_defaultsToSerbian() {
        assertThat(LocaleResolver.of(null, null)).isEqualTo(Locale.forLanguageTag("sr"));
    }
}
