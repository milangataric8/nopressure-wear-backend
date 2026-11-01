package rs.nopressurewear.media.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Arrays;

@ConfigurationProperties("app.s3")
public record S3Properties(String bucket, String region, String publicBaseUrl, String prefix) {
    public S3Properties {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3_MEDIA_BUCKET must be configured when S3 storage is enabled");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS_REGION must be configured when S3 storage is enabled");
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalArgumentException("S3_PUBLIC_BASE_URL must be configured when S3 storage is enabled");
        }
        bucket = bucket.trim();
        region = region.trim();
        publicBaseUrl = publicBaseUrl.trim().replaceAll("/+$", "");
        URI base = URI.create(publicBaseUrl);
        if (!"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null
                || base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null
                || !base.getRawPath().matches("[A-Za-z0-9/_.-]*")
                || Arrays.stream(base.getPath().split("/")).anyMatch(segment -> segment.equals(".") || segment.equals(".."))) {
            throw new IllegalArgumentException("S3_PUBLIC_BASE_URL must be an HTTPS media base URL without credentials, query or fragment");
        }
        prefix = prefix == null ? "products" : prefix.trim().replaceAll("^/+|/+$", "");
        if (!prefix.isEmpty() && (!prefix.matches("[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*")
                || Arrays.stream(prefix.split("/")).anyMatch(segment -> segment.equals(".") || segment.equals("..")))) {
            throw new IllegalArgumentException("S3_MEDIA_PREFIX must contain safe path segments");
        }
    }
}
