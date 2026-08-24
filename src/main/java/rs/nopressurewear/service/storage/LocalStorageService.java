package rs.nopressurewear.service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.nopressurewear.service.FileStorageService;
import rs.nopressurewear.service.RemoveBgService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile("!prod")
@RequiredArgsConstructor
public class LocalStorageService implements StorageProvider {

    private final FileStorageService fileStorageService;
    private final RemoveBgService removeBgService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, boolean removeBg) {
        if (removeBg) {
            try {
                byte[] processedBytes = removeBgService.removeBackground(file);
                String fileName = UUID.randomUUID() + ".png";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Files.write(uploadPath.resolve(fileName), processedBytes);
                return "/uploads/products/" + fileName;
            } catch (Exception e) {
                System.out.println("Background removal failed: " + e.getMessage());
                // fall through to normal upload
            }
        }
        return fileStorageService.storeFile(file);
    }

    @Override
    public void delete(String url) {
        // no-op for local dev; file deletion on disk not required
    }
}
