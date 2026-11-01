package rs.nopressurewear.media.internal.config;

import com.cloudinary.Cloudinary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "app.storage.cloudinary-enabled", havingValue = "true")
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(); // reads CLOUDINARY_URL env var automatically
    }
}
