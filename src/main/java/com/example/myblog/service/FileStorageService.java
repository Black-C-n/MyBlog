package com.example.myblog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String relativeDirectory) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalName.substring(dot).toLowerCase();
        }
        if (!extension.matches("\\.[a-z0-9]{1,10}")) {
            extension = "";
        }

        String safeDirectory = relativeDirectory.replace('\\', '/').replace("..", "");
        Path directory = uploadRoot.resolve(safeDirectory).normalize();
        if (!directory.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法上传目录");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }
        return safeDirectory + "/" + fileName;
    }
}
