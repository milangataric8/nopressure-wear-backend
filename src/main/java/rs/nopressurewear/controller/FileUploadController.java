package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.nopressurewear.service.storage.StorageProvider;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.nonNull;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final StorageProvider storageProvider;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            "mp4", "webm", "ogg", "mov", "avi", "mkv"
    );

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private boolean isAllowedImage(MultipartFile file) {
        String ct = file.getContentType();
        if (nonNull(ct) && ct.startsWith("image/")) return true;
        return ALLOWED_IMAGE_EXTENSIONS.contains(getExtension(file.getOriginalFilename()));
    }

    private boolean isAllowedVideo(MultipartFile file) {
        String ct = file.getContentType();
        if (nonNull(ct) && ct.startsWith("video/")) return true;
        return ALLOWED_VIDEO_EXTENSIONS.contains(getExtension(file.getOriginalFilename()));
    }

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "removeBackground", defaultValue = "false") boolean removeBackground) {

        if (file.isEmpty()) throw new RuntimeException("File is empty");
        if (!isAllowedImage(file)) {
            throw new RuntimeException("Only image files are allowed (jpg, jpeg, png, gif, webp, bmp)");
        }

        String url = storageProvider.store(file, removeBackground);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/video")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> uploadVideo(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) throw new RuntimeException("File is empty");
        if (!isAllowedVideo(file)) {
            throw new RuntimeException("Only video files are allowed (mp4, webm, ogg, mov, avi, mkv)");
        }

        String url = storageProvider.store(file, false);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
