package rs.nopressurewear.media.internal.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class S3PropertiesTest {
    @ParameterizedTest
    @ValueSource(strings = {"https://user:password@media.test", "https://media.test?key=value",
            "https://media.test/#fragment", "http://media.test", "https://media.test/a/../b", "https://media.test/%2e%2e"})
    void rejectsUnsafePublicBase(String base) {
        assertThatThrownBy(() -> new S3Properties("bucket", "eu-north-1", base, "products"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "products/../other", "products//nested", "%2e%2e", "products?secret=value"})
    void rejectsUnsafePrefix(String prefix) {
        assertThatThrownBy(() -> new S3Properties("bucket", "eu-north-1", "https://media.test", prefix))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesBaseAndPrefixOnceAtStartup() {
        var properties = new S3Properties(" bucket ", " eu-north-1 ", "https://media.test/media/", "/catalog/products/");
        assertThat(properties.bucket()).isEqualTo("bucket");
        assertThat(properties.region()).isEqualTo("eu-north-1");
        assertThat(properties.publicBaseUrl()).isEqualTo("https://media.test/media");
        assertThat(properties.prefix()).isEqualTo("catalog/products");
    }
}
