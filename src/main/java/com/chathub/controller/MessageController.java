package com.chathub.controller;

import com.chathub.dto.MessageCreateRequest;
import com.chathub.dto.MessageUpdateRequest;
import com.chathub.dto.ReactionRequest;
import com.chathub.model.User;
import com.chathub.service.FileService;
import com.chathub.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @RequestParam String channel_id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getMessages(channel_id, limit, before, user));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMessage(
            @Valid @RequestBody MessageCreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.createMessage(req, user));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<Map<String, Object>> updateMessage(
            @PathVariable String messageId,
            @Valid @RequestBody MessageUpdateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.updateMessage(messageId, req, user));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, Object>> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.deleteMessage(messageId, user));
    }

    @PostMapping("/{messageId}/react")
    public ResponseEntity<Map<String, Object>> react(
            @PathVariable String messageId,
            @Valid @RequestBody ReactionRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.toggleReaction(messageId, req.getEmoji(), user));
    }

    @PostMapping("/{messageId}/pin")
    public ResponseEntity<Map<String, Object>> togglePin(
            @PathVariable String messageId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.togglePin(messageId, user));
    }

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.markMessageRead(messageId, user));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(
            @RequestParam String channel_id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.markAllRead(channel_id, user));
    }

    @GetMapping("/{messageId}/thread")
    public ResponseEntity<List<Map<String, Object>>> getThread(
            @PathVariable String messageId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getThread(messageId, user));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam String query,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.searchMessages(query, user));
    }
}
