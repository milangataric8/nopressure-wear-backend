package rs.nopressurewear.platform.util;

import org.junit.jupiter.api.Test;
import rs.nopressurewear.platform.config.MessageConfig;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationResourcesTest {
    private Properties load(String file) throws Exception {
        Properties values = new Properties();
        try (var stream = getClass().getResourceAsStream("/" + file)) {
            assertThat(stream).as(file).isNotNull();
            values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return values;
    }

    private TreeSet<String> parameters(String value) {
        TreeSet<String> result = new TreeSet<>();
        Pattern.compile("\\{(\\d+)(?:,[^}]*)?}").matcher(value).results()
                .forEach(match -> result.add(match.group(1)));
        return result;
    }

    @Test
    void emailDictionariesHaveMatchingKeysAndParametersAndSerbianBase() throws Exception {
        Properties sr = load("messages_sr.properties");
        Properties en = load("messages_en.properties");
        assertThat(en.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(sr.stringPropertyNames());
        assertThat(load("messages.properties")).isEqualTo(sr);
        for (String key : sr.stringPropertyNames()) {
            assertThat(sr.getProperty(key)).as(key + " sr").isNotBlank();
            assertThat(en.getProperty(key)).as(key + " en").isNotBlank();
            assertThat(parameters(en.getProperty(key))).as(key).isEqualTo(parameters(sr.getProperty(key)));
        }
    }

    @Test
    void unsupportedLocaleResolvesSerbianEmailText() {
        var source = new MessageConfig().messageSource();
        assertThat(source.getMessage("email.verification.subject", null, Locale.GERMAN))
                .isEqualTo("Potvrdite vaš email");
        assertThat(source.getMessage("email.verification.subject", null, Locale.ENGLISH))
                .isEqualTo("Verify your email");
    }
}
