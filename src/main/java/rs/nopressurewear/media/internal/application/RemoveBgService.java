package rs.nopressurewear.media.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RemoveBgService {

    @Value("${app.removebg.api-key:}")
    private String apiKey;

    @Value("${app.removebg.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public byte[] removeBackground(MultipartFile file) {
        if (!enabled) {
            throw new IllegalStateException("Background removal is disabled");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Background removal API key is not configured");
        }
        try {
            WebClient client = WebClient.builder()
                    .baseUrl("https://api.remove.bg/v1.0")
                    // Processed PNGs routinely exceed WebClient's default 256 KiB limit.
                    .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                    .build();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image_file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("size", "auto");

            return client.post()
                    .uri("/removebg")
                    .header("X-Api-Key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove background", e);
        }
    }
}
