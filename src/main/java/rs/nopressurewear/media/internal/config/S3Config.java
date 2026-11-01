package rs.nopressurewear.media.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
@ConditionalOnProperty(name = "app.storage.s3-enabled", havingValue = "true")
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {
    @Bean
    public S3Client s3Client(S3Properties properties) {
        // Default credential provider chain: EC2 IAM role in production, never static keys.
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(30))
                        .apiCallTimeout(Duration.ofSeconds(60)).build())
                .build();
    }
}
