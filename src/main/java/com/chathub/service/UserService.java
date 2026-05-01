package com.chathub.service;

import com.chathub.dto.UserUpdateRequest;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.InviteLinkRepository;
import com.chathub.repository.MessageRepository;
import com.chathub.repository.UserRepository;
import com.chathub.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final ChatWebSocketHandler wsHandler;

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AuthService::toPublicUser)
                .collect(Collectors.toList());
    }

    public Map<String, Object> updateUser(User currentUser, UserUpdateRequest req) {
        User user = userRepository.findByCustomId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            user.setUsername(sanitize(req.getUsername()));
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (req.getBio() != null) {
            user.setBio(sanitize(req.getBio()));
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getAvatarColor() != null) {
            user.setAvatarColor(req.getAvatarColor());
        }

        userRepository.save(user);

        if (req.getStatus() != null) {
            wsHandler.broadcastUserStatus(user.getId(), user.isOnline(), req.getStatus());
        }

        return AuthService.toPublicUser(user);
    }

    public void deleteAccount(User currentUser) {
        // Remove from all channels
        channelRepository.findByMembersContaining(currentUser.getId()).forEach(ch -> {
            ch.getMembers().remove(currentUser.getId());
            channelRepository.save(ch);
        });

        // Delete messages
        messageRepository.findAll().stream()
                .filter(m -> currentUser.getId().equals(m.getUserId()))
                .forEach(messageRepository::delete);

        // Deactivate invite links
        inviteLinkRepository.findAll().stream()
                .filter(i -> currentUser.getId().equals(i.getCreatedBy()))
                .forEach(i -> {
                    i.setActive(false);
                    inviteLinkRepository.save(i);
                });

        // Delete user
        userRepository.findAll().stream()
                .filter(u -> currentUser.getId().equals(u.getId()))
                .findFirst()
                .ifPresent(userRepository::delete);
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.strip().replace("<", "&lt;").replace(">", "&gt;");
    }
}
