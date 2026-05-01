package com.chathub.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "image/svg+xml", "video/mp4", "video/webm",
            "application/pdf", "text/plain", "text/csv",
            "application/zip", "application/x-zip-compressed",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",   // .docx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",        // .xlsx
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",// .pptx
            "application/msword",                                                        // .doc
            "application/vnd.ms-excel",                                                  // .xls
            "audio/mpeg", "audio/wav", "audio/ogg"
    );

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8000}")
    private String baseUrl;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "No file provided"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("detail", "File size exceeds 10MB limit"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("detail", "File type not allowed: " + contentType));
        }

        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(uploadPath);

            // Generate a unique filename preserving extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Save the file
            Path targetPath = uploadPath.resolve(filename);
            file.transferTo(targetPath.toFile());

            // Return the URL that WebConfig serves at /uploads/**
            String currentBaseUrl = baseUrl;
            if (baseUrl == null || baseUrl.isEmpty() || baseUrl.contains("localhost")) {
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                currentBaseUrl = scheme + "://" + serverName + ":" + serverPort;
            }

            String url = currentBaseUrl + "/uploads/" + filename;
            return ResponseEntity.ok(Map.of(
                    "url", url,
                    "filename", filename
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("detail", "Failed to save file: " + e.getMessage()));
        }
    }
}
