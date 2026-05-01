package com.chathub.controller;

import com.chathub.dto.ChannelCreateRequest;
import com.chathub.dto.ChannelUpdateRequest;
import com.chathub.dto.MemberActionRequest;
import com.chathub.model.User;
import com.chathub.service.ChannelService;
import com.chathub.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createChannel(
            @Valid @RequestBody ChannelCreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.createChannel(req, user));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getChannels(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.getChannels(user));
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<Map<String, Object>> getChannel(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.getChannel(channelId, user));
    }

    @PutMapping("/{channelId}")
    public ResponseEntity<Map<String, Object>> updateChannel(
            @PathVariable String channelId,
            @RequestBody ChannelUpdateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.updateChannel(channelId, req, user));
    }

    @PostMapping("/{channelId}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.toggleFavorite(channelId, user));
    }

    @PostMapping("/{channelId}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable String channelId,
            @RequestBody MemberActionRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.addMember(channelId, req.getUserId(), user));
    }

    @DeleteMapping("/{channelId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.removeMember(channelId, userId, user));
    }

    @GetMapping("/{channelId}/members")
    public ResponseEntity<Map<String, Object>> getMembers(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.getMembers(channelId, user));
    }

    @PostMapping("/{channelId}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(channelService.markRead(channelId, user));
    }

    // ── Message endpoints scoped to channel (matches frontend URL pattern) ────

    /**
     * GET /api/channels/{channelId}/messages?limit=50[&before=timestamp]
     * Frontend calls this URL — previously the backend only had /api/messages?channel_id=...
     */
    @GetMapping("/{channelId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getMessages(channelId, limit, before, user));
    }

    /**
     * POST /api/channels/{channelId}/read-all
     * Frontend calls this URL to mark all messages in a channel as read.
     */
    @PostMapping("/{channelId}/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.markAllRead(channelId, user));
    }
}
