package com.chathub.controller;

import com.chathub.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileService fileService;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "No file provided"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("detail", "File type not allowed: " + contentType));
        }

        try {
            Map<String, Object> result = fileService.uploadFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("detail", e.getMessage()));
        }
    }
}
