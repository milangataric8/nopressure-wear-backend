package rs.nopressurewear.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.nopressurewear.service.RemoveBgService;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageProvider {

    private final Cloudinary cloudinary;
    private final RemoveBgService removeBgService;

    @Value("${app.cloudinary.folder:nopressure/products}")
    private String folder;

    @Override
    public String store(MultipartFile file, boolean removeBg) {
        try {
            byte[] bytes;
            if (removeBg) {
                bytes = removeBgService.removeBackground(file);
            } else {
                bytes = file.getBytes();
            }

            Map<?, ?> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.contains("/image/upload/")) return;
        try {
            String publicId = extractPublicId(url);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception ignored) {
            // deletion failure should not break the app
        }
    }

    // Parses: .../upload/v1234567890/nopressure/products/abc.jpg → nopressure/products/abc
    private String extractPublicId(String url) {
        Pattern pattern = Pattern.compile("/upload/(?:v\\d+/)?(.+?)(?:\\.[^./]+)?$");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
