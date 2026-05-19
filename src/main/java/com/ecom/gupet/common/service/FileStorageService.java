package com.ecom.gupet.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public List<String> uploadMultiple(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        Path uploadPath = Paths.get(uploadDir, "pets");

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath);

                    // Trả về URL tương đối (frontend sẽ dùng /uploads/pets/...)
                    urls.add("/uploads/pets/" + fileName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
        }
        return urls;
    }

    // Xóa file nếu cần sau này
    public void deleteFile(String fileUrl) {
        // Implement nếu cần
    }
}