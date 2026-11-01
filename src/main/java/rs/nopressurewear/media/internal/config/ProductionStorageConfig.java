package rs.nopressurewear.media.internal.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionStorageConfig {
    // Validate before either client or StorageProvider is instantiated, including their credentials/config.
    @Bean
    static BeanFactoryPostProcessor validateProductionStorage(Environment environment) {
        return beanFactory -> {
            boolean s3 = environment.getProperty("app.storage.s3-enabled", Boolean.class, false);
            boolean cloudinary = environment.getProperty("app.storage.cloudinary-enabled", Boolean.class, false);
            if (s3 == cloudinary) {
                throw new IllegalStateException("Exactly one production media storage provider must be enabled "
                        + "(S3_STORAGE_ENABLED / CLOUDINARY_ENABLED)");
            }
        };
    }
}
