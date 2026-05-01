package com.chathub.controller;

import com.chathub.dto.UserUpdateRequest;
import com.chathub.model.User;
import com.chathub.service.AuthService;
import com.chathub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(AuthService.toPublicUser(user));
    }

    @PutMapping("/users/me")
    public ResponseEntity<Map<String, Object>> updateMe(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserUpdateRequest req) {
        return ResponseEntity.ok(userService.updateUser(user, req));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<Map<String, Object>> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }



    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "ChatHub Java Backend"));
    }
}
