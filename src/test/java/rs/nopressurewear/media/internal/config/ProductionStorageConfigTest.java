package rs.nopressurewear.media.internal.config;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import rs.nopressurewear.media.internal.application.FileStorageService;
import rs.nopressurewear.media.internal.application.RemoveBgService;
import rs.nopressurewear.media.internal.storage.*;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductionStorageConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionStorageConfig.class, CloudinaryConfig.class, S3Config.class,
                    S3StorageService.class, CloudinaryStorageService.class, LocalStorageService.class, FileStorageService.class)
            .withBean(RemoveBgService.class, () -> mock(RemoveBgService.class))
            .withPropertyValues("spring.profiles.active=prod", "app.s3.bucket=media-test",
                    "app.s3.region=eu-north-1", "app.s3.public-base-url=https://media.test", "app.upload.dir=target/test-uploads");

    @Test
    void selectsOnlyS3WithoutCloudinaryConfiguration() {
        runner.withPropertyValues("app.storage.s3-enabled=true", "app.storage.cloudinary-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(StorageProvider.class)
                            .hasSingleBean(S3StorageService.class).hasSingleBean(S3Client.class)
                            .doesNotHaveBean(Cloudinary.class).doesNotHaveBean(CloudinaryStorageService.class)
                            .doesNotHaveBean(LocalStorageService.class);
                });
    }

    @Test
    void selectsOnlyCloudinaryWithoutS3Configuration() {
        runner.withPropertyValues("app.storage.s3-enabled=false", "app.storage.cloudinary-enabled=true",
                        "app.s3.bucket=", "app.s3.public-base-url=")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(StorageProvider.class)
                        .hasSingleBean(CloudinaryStorageService.class).hasSingleBean(Cloudinary.class)
                        .doesNotHaveBean(S3Client.class).doesNotHaveBean(S3StorageService.class));
    }

    @Test
    void rejectsBothEnabledBeforeResolvingProviderConfig() {
        invalid(true, true);
    }

    @Test
    void rejectsNeitherEnabled() {
        invalid(false, false);
    }

    private void invalid(boolean s3, boolean cloudinary) {
        runner.withPropertyValues("app.storage.s3-enabled=" + s3, "app.storage.cloudinary-enabled=" + cloudinary,
                        "app.s3.bucket=", "app.s3.public-base-url=")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("Exactly one production media storage provider must be enabled"));
    }

    @Test
    void localStorageIgnoresProductionFlags() {
        runner.withPropertyValues("spring.profiles.active=local", "app.storage.s3-enabled=true",
                        "app.storage.cloudinary-enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(StorageProvider.class)
                        .hasSingleBean(LocalStorageService.class).doesNotHaveBean(S3Client.class).doesNotHaveBean(Cloudinary.class));
    }

    @Test
    void enabledS3RequiresBucket() {
        runner.withPropertyValues("app.storage.s3-enabled=true", "app.storage.cloudinary-enabled=false", "app.s3.bucket=")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("S3_MEDIA_BUCKET must be configured"));
    }
}
