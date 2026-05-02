package com.chathub.service;

import com.chathub.dto.LoginRequest;
import com.chathub.dto.PasswordChangeRequest;
import com.chathub.dto.RegisterRequest;
import com.chathub.model.Channel;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.UserRepository;
import com.chathub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final List<String> AVATAR_COLORS = List.of(
        "#7c3aed", "#0d9488", "#ec4899", "#f59e0b", "#3b82f6", "#ef4444"
    );

    public Map<String, Object> register(RegisterRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        String username = req.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
        }

        Random rand = new Random();
        User user = User.builder()
                .email(email)
                .username(sanitize(username))
                .hashedPassword(passwordEncoder.encode(req.getPassword()))
                .avatarColor(AVATAR_COLORS.get(rand.nextInt(AVATAR_COLORS.size())))
                .build();

        userRepository.save(user);

        // Auto-join all existing public channels (mirrors channel creation behaviour where
        // every user is added automatically — just like "general").
        List<Channel> publicChannels = channelRepository.findAll().stream()
                .filter(ch -> !ch.isDm() && !ch.isPrivate())
                .collect(java.util.stream.Collectors.toList());

        if (publicChannels.isEmpty()) {
            // First ever user — create the default "general" channel
            Channel general = Channel.builder()
                    .name("general")
                    .description("General discussion")
                    .isDm(false)
                    .members(new ArrayList<>(List.of(user.getId())))
                    .createdBy(user.getId())
                    .build();
            channelRepository.save(general);
        } else {
            // Add the new user to every existing public channel
            for (Channel ch : publicChannels) {
                if (!ch.getMembers().contains(user.getId())) {
                    ch.getMembers().add(user.getId());
                    channelRepository.save(ch);
                }
            }
        }

        String token = jwtUtil.generateToken(user.getId());
        return buildTokenResponse(token, user);
    }

    public Map<String, Object> login(LoginRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password"));

        // Guard: Python-migrated users may have null/empty hashed_password
        String hash = user.getHashedPassword();
        if (hash == null || hash.isBlank() || !passwordEncoder.matches(req.getPassword(), hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        }

        String token = jwtUtil.generateToken(user.getId());
        return buildTokenResponse(token, user);
    }

    public void changePassword(User currentUser, PasswordChangeRequest req) {
        User userDoc = userRepository.findByCustomId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String existingHash = userDoc.getHashedPassword();
        if (existingHash == null || existingHash.isBlank() || !passwordEncoder.matches(req.getCurrentPassword(), existingHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        userDoc.setHashedPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(userDoc);
    }

    private Map<String, Object> buildTokenResponse(String token, User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "bearer");
        response.put("user", toPublicUser(user));
        return response;
    }

    public static Map<String, Object> toPublicUser(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("username", user.getUsername());
        map.put("is_online", user.isOnline());
        map.put("status", user.getStatus());
        map.put("last_seen", user.getLastSeen());
        map.put("avatar_color", user.getAvatarColor());
        map.put("avatar_url", user.getAvatarUrl());
        map.put("bio", user.getBio());
        map.put("created_at", user.getCreatedAt());
        return map;
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.strip().replace("<", "&lt;").replace(">", "&gt;");
    }
}
