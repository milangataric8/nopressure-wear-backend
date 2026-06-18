package rs.nopressurewear.service;

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

    @Value("${app.removebg.api-key}")
    private String apiKey;

    public byte[] removeBackground(MultipartFile file) {
        try {
            WebClient client = WebClient.builder()
                    .baseUrl("https://api.remove.bg/v1.0")
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
            throw new RuntimeException("Failed to remove background: " + e.getMessage());
        }
    }
}