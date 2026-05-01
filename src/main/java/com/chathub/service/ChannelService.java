package com.chathub.service;

import com.chathub.dto.ChannelCreateRequest;
import com.chathub.dto.ChannelUpdateRequest;
import com.chathub.model.Channel;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.UserRepository;
import com.chathub.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ChatWebSocketHandler wsHandler;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> createChannel(ChannelCreateRequest req, User currentUser) {
        List<String> members = req.getMembers() != null ? new ArrayList<>(req.getMembers()) : new ArrayList<>();
        if (!members.contains(currentUser.getId())) members.add(currentUser.getId());

        if (req.getIsDm() != null && req.getIsDm()) {
            Optional<Channel> existing = channelRepository.findDmByMembers(members, members.size());
            if (existing.isPresent()) {
                return toMap(existing.get());
            }
        } else if (!Boolean.TRUE.equals(req.getIsPrivate())) {
            // Public channel: automatically add ALL registered users so anyone can chat,
            // just like the "general" channel behaviour.
            List<String> allUserIds = userRepository.findAll().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            for (String uid : allUserIds) {
                if (!members.contains(uid)) members.add(uid);
            }
        }

        // Validate private channel requirements
        if (Boolean.TRUE.equals(req.getIsPrivate()) && (req.getPassword() == null || req.getPassword().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for private channels");
        }

        System.out.println("Creating channel: " + req.getName() + ", Private: " + req.getIsPrivate() + ", Has Password: " + (req.getPassword() != null && !req.getPassword().isBlank()));

        Channel channel = Channel.builder()
                .name(sanitize(req.getName()))
                .description(req.getDescription() != null ? sanitize(req.getDescription()) : "")
                .isDm(Boolean.TRUE.equals(req.getIsDm()))
                .isPrivate(Boolean.TRUE.equals(req.getIsPrivate()))
                .password(Boolean.TRUE.equals(req.getIsPrivate()) ? passwordEncoder.encode(req.getPassword()) : null)
                .members(members)
                .createdBy(currentUser.getId())
                .build();

        channelRepository.save(channel);
        return toMap(channel);
    }

    public List<Map<String, Object>> getChannels(User currentUser) {
        return channelRepository.findByMembersContaining(currentUser.getId())
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> getChannel(String channelId, User currentUser) {
        Channel ch = findOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        return toMap(ch);
    }

    public Map<String, Object> updateChannel(String channelId, ChannelUpdateRequest req, User currentUser) {
        Channel ch = findOrThrow(channelId);
        if (!ch.getCreatedBy().equals(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the channel creator can edit");
        if (ch.isDm())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit DM channels");

        if (req.getName() != null) ch.setName(sanitize(req.getName()));
        if (req.getDescription() != null) ch.setDescription(sanitize(req.getDescription()));
        if (req.getIsPrivate() != null) {
            // If changing to private, ensure we have a password (either existing or new)
            if (req.getIsPrivate() && (req.getPassword() == null || req.getPassword().isBlank()) && (ch.getPassword() == null || ch.getPassword().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required when making a channel private");
            }
            ch.setPrivate(req.getIsPrivate());
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            ch.setPassword(passwordEncoder.encode(req.getPassword()));
        } else if (Boolean.FALSE.equals(req.getIsPrivate())) {
            // If explicitly making it public, clear the password
            ch.setPassword(null);
        }
        channelRepository.save(ch);
        return toMap(ch);
    }

    public Map<String, Object> toggleFavorite(String channelId, User currentUser) {
        Channel ch = findOrThrow(channelId);
        if (ch.getIsFavorite() == null) ch.setIsFavorite(new ArrayList<>());

        boolean wasFav = ch.getIsFavorite().contains(currentUser.getId());
        if (wasFav) ch.getIsFavorite().remove(currentUser.getId());
        else ch.getIsFavorite().add(currentUser.getId());
        channelRepository.save(ch);
        // Return the full channel so the frontend can update its state without a full refetch
        return toMap(ch);
    }

    public Map<String, Object> addMember(String channelId, String targetUserId, User currentUser) {
        Channel ch = findOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        if (ch.isDm())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add members to DM channels");

        User target = userRepository.findAll().stream()
                .filter(u -> targetUserId.equals(u.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (ch.getMembers().contains(targetUserId))
            return Map.of("success", true, "message", "User already a member");

        ch.getMembers().add(targetUserId);
        channelRepository.save(ch);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "channel_updated");
        event.put("channel_id", channelId);
        event.put("action", "member_added");
        event.put("user_id", targetUserId);
        event.put("username", target.getUsername());
        wsHandler.broadcastToChannel(event, channelId);

        return Map.of("success", true, "message", target.getUsername() + " added to channel");
    }

    public Map<String, Object> removeMember(String channelId, String userId, User currentUser) {
        Channel ch = findOrThrow(channelId);
        if (ch.isDm())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove members from DM channels");
        if (!currentUser.getId().equals(ch.getCreatedBy()) && !currentUser.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        if (!ch.getMembers().contains(userId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a member");
        if (userId.equals(ch.getCreatedBy()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove channel creator");

        String username = userRepository.findAll().stream()
                .filter(u -> userId.equals(u.getId())).findFirst()
                .map(User::getUsername).orElse("Unknown");

        ch.getMembers().remove(userId);
        channelRepository.save(ch);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "channel_updated");
        event.put("channel_id", channelId);
        event.put("action", "member_removed");
        event.put("user_id", userId);
        event.put("username", username);
        wsHandler.broadcastToChannel(event, channelId);

        return Map.of("success", true);
    }

    public Map<String, Object> getMembers(String channelId, User currentUser) {
        Channel ch = findOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        List<Map<String, Object>> members = userRepository.findAll().stream()
                .filter(u -> ch.getMembers().contains(u.getId()))
                .map(AuthService::toPublicUser)
                .collect(Collectors.toList());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("members", members);
        result.put("created_by", ch.getCreatedBy());
        return result;
    }

    public Map<String, Object> markRead(String channelId, User currentUser) {
        Channel ch = findOrThrow(channelId);
        ch.getUnreadCount().put(currentUser.getId(), 0);
        channelRepository.save(ch);
        return Map.of("success", true);
    }

    private Channel findOrThrow(String channelId) {
        return channelRepository.findByChannelId(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
    }

    public Map<String, Object> toMap(Channel ch) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ch.getId());
        map.put("name", ch.getName());
        map.put("description", ch.getDescription());
        map.put("is_dm", ch.isDm());
        map.put("is_private", ch.isPrivate());
        map.put("members", ch.getMembers());
        map.put("created_by", ch.getCreatedBy());
        map.put("created_at", ch.getCreatedAt());
        map.put("is_favorite", ch.getIsFavorite());
        map.put("is_muted", ch.getIsMuted());
        map.put("unread_count", ch.getUnreadCount());
        return map;
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.strip().replace("<", "&lt;").replace(">", "&gt;");
    }
}
