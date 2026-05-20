package com.ecom.gupet.common.service;

import com.ecom.gupet.modules.pet.entity.PetImage;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation.resolve("pets"));
            log.info("📁 Upload directory initialized: {}", rootLocation);
        } catch (Exception e) {
            log.error("❌ Cannot create upload directory", e);
        }
    }

    /**
     * Upload nhiều ảnh + tạo thumbnail (phiên bản an toàn)
     */
    public List<PetImage> uploadPetImagesWithThumbnails(MultipartFile[] files) {
        List<PetImage> petImages = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file.isEmpty()) continue;

            try {
                byte[] fileBytes = file.getBytes(); // Đọc một lần duy nhất

                String originalUrl = saveFileFromBytes(fileBytes, file.getOriginalFilename(), "pets");
                String thumbnailUrl = createThumbnailFromBytes(fileBytes, "pets");

                PetImage petImage = PetImage.builder()
                        .imageUrl(originalUrl)
                        .thumbnailUrl(thumbnailUrl)
                        .displayOrder(i)
                        .build();

                petImages.add(petImage);
            } catch (Exception e) {
                log.error("Upload failed for file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Upload ảnh thất bại: " + file.getOriginalFilename(), e);
            }
        }
        return petImages;
    }

    private String saveFileFromBytes(byte[] bytes, String originalName, String subDir) throws IOException {
        String fileName = UUID.randomUUID() + "_" + StringUtils.cleanPath(originalName);
        Path targetPath = rootLocation.resolve(subDir).resolve(fileName);

        Files.write(targetPath, bytes);
        return "/uploads/" + subDir + "/" + fileName;
    }

    private String createThumbnailFromBytes(byte[] bytes, String subDir) throws IOException {
        String thumbName = UUID.randomUUID() + "_thumb.jpg";
        Path thumbPath = rootLocation.resolve(subDir).resolve(thumbName);

        Thumbnails.of(new ByteArrayInputStream(bytes))
                .size(300, 300)
                .outputQuality(0.85)
                .toFile(thumbPath.toFile());

        return "/uploads/" + subDir + "/" + thumbName;
    }

    // Phương thức cũ (nếu bạn còn dùng ở nơi khác)
    public List<String> uploadMultiple(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();
                    urls.add(saveFileFromBytes(bytes, file.getOriginalFilename(), "pets"));
                } catch (Exception e) {
                    throw new RuntimeException("Upload file thất bại", e);
                }
            }
        }
        return urls;
    }

    /**
     * Xóa file theo URL
     */
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) return;

        try {
            // url dạng: /uploads/pets/xxxx.jpg
            String relativePath = url.replace("/uploads/", "");
            Path filePath = rootLocation.resolve(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Đã xóa file: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Không xóa được file: {}", url, e);
        }
    }
}

//package com.ecom.gupet.common.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class FileStorageService {
//
//    @Value("${app.upload.dir:uploads}")
//    private String uploadDir;
//
//    public List<String> uploadMultiple(MultipartFile[] files) {
//        List<String> urls = new ArrayList<>();
//        Path uploadPath = Paths.get(uploadDir, "pets");
//
//        try {
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            for (MultipartFile file : files) {
//                if (!file.isEmpty()) {
//                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//                    Path filePath = uploadPath.resolve(fileName);
//                    Files.copy(file.getInputStream(), filePath);
//
//                    // Trả về URL tương đối (frontend sẽ dùng /uploads/pets/...)
//                    urls.add("/uploads/pets/" + fileName);
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
//        }
//        return urls;
//    }
//
//    // Xóa file nếu cần sau này
//    public void deleteFile(String fileUrl) {
//        // Implement nếu cần
//    }
//}