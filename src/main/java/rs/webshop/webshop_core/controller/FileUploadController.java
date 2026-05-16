package rs.webshop.webshop_core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.webshop.webshop_core.service.FileStorageService;
import rs.webshop.webshop_core.service.RemoveBgService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final RemoveBgService removeBgService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "removeBackground", defaultValue = "false") boolean removeBackground) {

        if (file.isEmpty()) throw new RuntimeException("File is empty");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        if (removeBackground) {
            try {
                byte[] processedBytes = removeBgService.removeBackground(file);
                String fileName = UUID.randomUUID() + ".png";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(fileName);
                Files.write(filePath, processedBytes);
                return ResponseEntity.ok(Map.of("url", "/uploads/products/" + fileName));
            } catch (Exception e) {
                System.out.println("Background removal failed: " + e.getMessage());
            }
        }

        String fileUrl = fileStorageService.storeFile(file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }

    @PostMapping("/video")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> uploadVideo(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new RuntimeException("Only video files are allowed");
        }

        String fileUrl = fileStorageService.storeFile(file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }
}