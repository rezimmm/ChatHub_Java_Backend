package com.chathub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private final Cloudinary cloudinary;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

    public Map<String, Object> uploadFile(MultipartFile file) {
        if (file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        if (file.getSize() > MAX_FILE_SIZE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Max size is 10MB.");

        try {
            // Upload to Cloudinary
            // We use resource_type: auto to handle images, videos, and raw files (PDFs, etc.)
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
                ObjectUtils.asMap("resource_type", "auto"));
            
            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String format = (String) uploadResult.get("format");
            String originalName = file.getOriginalFilename();

            log.info("File uploaded to Cloudinary: {} (ID: {})", url, publicId);

            return Map.of(
                    "url", url,
                    "file_url", url, // Backwards compatibility
                    "filename", originalName != null ? originalName : publicId,
                    "public_id", publicId,
                    "file_type", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
            );
        } catch (IOException e) {
            log.error("Failed to upload to Cloudinary: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to cloud storage");
        }
    }
}
