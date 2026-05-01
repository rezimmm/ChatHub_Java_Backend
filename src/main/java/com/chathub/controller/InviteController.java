package com.chathub.controller;

import com.chathub.dto.InviteCreateRequest;
import com.chathub.dto.InviteJoinRequest;
import com.chathub.model.User;
import com.chathub.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @PostMapping("/channels/{channelId}")
    public ResponseEntity<Map<String, Object>> createInvite(
            @PathVariable String channelId,
            @RequestBody InviteCreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inviteService.createInvite(channelId, req, user));
    }

    @GetMapping("/channels/{channelId}")
    public ResponseEntity<List<Map<String, Object>>> listInvites(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inviteService.listInvites(channelId, user));
    }

    @DeleteMapping("/{inviteId}")
    public ResponseEntity<Map<String, Object>> revokeInvite(
            @PathVariable String inviteId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inviteService.revokeInvite(inviteId, user));
    }

    @GetMapping("/{token}/info")
    public ResponseEntity<Map<String, Object>> getInviteInfo(@PathVariable String token) {
        return ResponseEntity.ok(inviteService.getInviteInfo(token));
    }

    @PostMapping("/{token}/join")
    public ResponseEntity<Map<String, Object>> joinViaInvite(
            @PathVariable String token,
            @RequestBody(required = false) InviteJoinRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inviteService.joinViaInvite(token, req, user));
    }
}
