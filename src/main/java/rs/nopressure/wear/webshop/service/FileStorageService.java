package rs.nopressure.wear.webshop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            System.out.println("Upload path: " + uploadPath.toAbsolutePath());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/products/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                Path filePath = Paths.get(fileUrl.substring(1));
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
}