package com.chathub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> uploadFile(MultipartFile file) {
        if (file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        if (file.getSize() > MAX_FILE_SIZE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Max size is 10MB.");

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0) ext = originalName.substring(dotIdx);

        String fileId = UUID.randomUUID().toString();
        String fileName = fileId + ext;

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            Files.write(filePath, file.getBytes());
            log.info("File saved: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }

        return Map.of(
                "file_url", "/uploads/" + fileName,
                "file_name", originalName,
                "file_type", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );
    }
}
