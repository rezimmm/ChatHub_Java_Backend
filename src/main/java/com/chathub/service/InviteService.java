package com.chathub.service;

import com.chathub.dto.InviteCreateRequest;
import com.chathub.model.Channel;
import com.chathub.model.InviteLink;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.InviteLinkRepository;
import com.chathub.repository.UserRepository;
import com.chathub.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.chathub.dto.InviteJoinRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteLinkRepository inviteLinkRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ChatWebSocketHandler wsHandler;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secret;

    public Map<String, Object> createInvite(String channelId, InviteCreateRequest req, User currentUser) {
        Channel ch = findChannelOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        if (ch.isDm())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create invite for DM channels");

        InviteLink invite = InviteLink.builder()
                .channelId(channelId)
                .createdBy(currentUser.getId())
                .maxUses(req.getMaxUses())
                .build();

        if (req.getExpiresInHours() != null) {
            invite.setExpiresAt(Instant.now().plus(req.getExpiresInHours(), ChronoUnit.HOURS).toString());
        } else if (req.getExpiresInMinutes() != null) {
            invite.setExpiresAt(Instant.now().plus(req.getExpiresInMinutes(), ChronoUnit.MINUTES).toString());
        }

        invite.setToken(generateHmacToken(channelId, invite.getId(), invite.getCreatedAt()));
        inviteLinkRepository.save(invite);
        return toMap(invite);
    }

    public List<Map<String, Object>> listInvites(String channelId, User currentUser) {
        Channel ch = findChannelOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        return inviteLinkRepository.findActiveByChannelId(channelId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> revokeInvite(String inviteId, User currentUser) {
        InviteLink invite = inviteLinkRepository.findByInviteId(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));
        Channel ch = findChannelOrThrow(invite.getChannelId());
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        invite.setActive(false);
        inviteLinkRepository.save(invite);
        return Map.of("success", true);
    }

    public Map<String, Object> getInviteInfo(String token) {
        InviteLink invite = inviteLinkRepository.findByTokenAndActive(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found or revoked"));
        validateInvite(invite, token);
        Channel ch = findChannelOrThrow(invite.getChannelId());

        String creatorUsername = userRepository.findByCustomId(invite.getCreatedBy())
                .map(User::getUsername)
                .orElse("Unknown User");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel_id", ch.getId());
        result.put("channel_name", ch.getName());
        result.put("channel_description", ch.getDescription());
        result.put("member_count", ch.getMembers().size());
        result.put("created_by_username", creatorUsername);
        result.put("expires_at", invite.getExpiresAt());
        result.put("use_count", invite.getUseCount());
        result.put("max_uses", invite.getMaxUses());
        result.put("is_private", ch.isPrivate());
        // A channel requires a password if it's private, even if for some reason the password itself is null (which shouldn't happen now)
        result.put("requires_password", ch.isPrivate() && ch.getPassword() != null && !ch.getPassword().isBlank());
        // Also add a flag to indicate if it's private but missing a password (for debugging)
        if (ch.isPrivate() && (ch.getPassword() == null || ch.getPassword().isBlank())) {
            result.put("warning", "Private channel has no password set");
        }
        return result;
    }

    public Map<String, Object> joinViaInvite(String token, InviteJoinRequest req, User currentUser) {
        InviteLink invite = inviteLinkRepository.findByTokenAndActive(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found or revoked"));
        validateInvite(invite, token);
        Channel ch = findChannelOrThrow(invite.getChannelId());

        // Enforce password check for all private channels
        if (ch.isPrivate()) {
            String password = req != null ? req.getPassword() : null;
            if (ch.getPassword() != null && !ch.getPassword().isBlank()) {
                if (password == null || !passwordEncoder.matches(password, ch.getPassword())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid channel password");
                }
            } else {
                System.out.println("Warning: Joining private channel " + ch.getId() + " which has no password.");
            }
        }

        if (ch.getMembers().contains(currentUser.getId())) {
            return Map.of("success", true, "channel_id", ch.getId(),
                    "channel_name", ch.getName(), "already_member", true);
        }

        ch.getMembers().add(currentUser.getId());
        channelRepository.save(ch);
        invite.setUseCount(invite.getUseCount() + 1);
        inviteLinkRepository.save(invite);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "channel_updated");
        event.put("channel_id", ch.getId());
        event.put("action", "member_added");
        event.put("user_id", currentUser.getId());
        event.put("username", currentUser.getUsername());
        wsHandler.broadcastToChannel(event, ch.getId());

        return Map.of("success", true, "channel_id", ch.getId(),
                "channel_name", ch.getName(), "already_member", false);
    }

    private void validateInvite(InviteLink invite, String token) {
        if (!generateHmacToken(invite.getChannelId(), invite.getId(), invite.getCreatedAt()).equals(token))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invite token");
        if (invite.getExpiresAt() != null && Instant.now().isAfter(Instant.parse(invite.getExpiresAt())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This invite link has expired");
        if (invite.getMaxUses() != null && invite.getUseCount() >= invite.getMaxUses())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This invite link has reached its maximum uses");
    }

    private String generateHmacToken(String channelId, String inviteId, String createdAt) {
        try {
            String message = channelId + ":" + inviteId + ":" + createdAt;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invite token", e);
        }
    }

    private Channel findChannelOrThrow(String channelId) {
        return channelRepository.findByChannelId(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
    }

    private Map<String, Object> toMap(InviteLink i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("channel_id", i.getChannelId());
        m.put("created_by", i.getCreatedBy());
        m.put("token", i.getToken());
        m.put("expires_at", i.getExpiresAt());
        m.put("max_uses", i.getMaxUses());
        m.put("use_count", i.getUseCount());
        m.put("is_active", i.isActive());
        m.put("created_at", i.getCreatedAt());
        return m;
    }
}
