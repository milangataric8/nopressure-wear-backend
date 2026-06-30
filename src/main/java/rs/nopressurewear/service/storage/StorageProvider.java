package rs.nopressurewear.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageProvider {
    String store(MultipartFile file, boolean removeBg);
    void delete(String url);
}
