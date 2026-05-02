package com.chathub.controller;

import com.chathub.model.Channel;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.InviteLinkRepository;
import com.chathub.repository.MessageRepository;
import com.chathub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final InviteLinkRepository inviteRepository;

    /**
     * RESET DATABASE for Production
     * This endpoint clears all messages, channels, and invites.
     * It can also clear all users except the one performing the reset.
     * PROTECTED by a simple token/password for safety during this transition.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDatabase(
            @RequestParam String confirm,
            @RequestParam(defaultValue = "false") boolean clearUsers,
            @AuthenticationPrincipal User user) {
        
        if (!"PRODUCTION_RESET_2024".equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid confirmation code");
        }

        // 1. Clear Messages
        messageRepository.deleteAll();
        
        // 2. Clear Invites
        inviteRepository.deleteAll();
        
        // 3. Clear Channels
        channelRepository.deleteAll();
        
        // 4. Clear Users (optional)
        if (clearUsers) {
            userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(user.getId()))
                    .forEach(u -> userRepository.delete(u));
        }

        // 5. Create Default "general" Channel
        Channel general = Channel.builder()
                .name("general")
                .description("General discussion for everyone")
                .isDm(false)
                .isPrivate(false)
                .members(new ArrayList<>(List.of(user.getId())))
                .createdBy(user.getId())
                .build();
        channelRepository.save(general);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Database cleared. 'general' channel created. Production ready."
        ));
    }
}
