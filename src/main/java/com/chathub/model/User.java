package com.chathub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String mongoId;

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Indexed(unique = true)
    private String email;

    private String username;

    private String hashedPassword;

    @Builder.Default
    @JsonProperty("is_online")
    @JsonAlias("is_online")
    private boolean isOnline = false;

    @Builder.Default
    private String status = "online";

    @Builder.Default
    @JsonProperty("last_seen")
    @JsonAlias("last_seen")
    private String lastSeen = Instant.now().toString();

    @Builder.Default
    @JsonProperty("avatar_color")
    @JsonAlias("avatar_color")
    private String avatarColor = "#7c3aed";

    @JsonProperty("avatar_url")
    @JsonAlias("avatar_url")
    private String avatarUrl;

    @Builder.Default
    private String bio = "";

    @Builder.Default
    @JsonProperty("created_at")
    @JsonAlias("created_at")
    private String createdAt = Instant.now().toString();
}
