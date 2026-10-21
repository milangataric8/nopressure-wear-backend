package rs.nopressurewear.platform.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

/**
 * Strategy B of {@code backend-message-i18n-spec.md} (section 4.2) — translated content for
 * consumers that can't run {@code react-i18next}: transactional emails today, PDFs/webhooks later.
 * Not to be confused with Strategy A's {@code ApiError.messageKey}, which the frontend resolves.
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.forLanguageTag("sr"));
        // otherwise a missing key falls back to the JVM/container's OS locale, which differs
        // between a local Windows dev box and the Railway container — never what we want.
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
