package com.chathub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "channels")
public class Channel {

    @Id
    private String mongoId;

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String name;

    @Builder.Default
    private String description = "";

    @Builder.Default
    @JsonProperty("is_dm")
    @JsonAlias("is_dm")
    private boolean isDm = false;

    @Builder.Default
    @JsonProperty("is_private")
    @JsonAlias("is_private")
    private boolean isPrivate = false;

    private String password;

    @Builder.Default
    private List<String> members = new ArrayList<>();

    private String createdBy;

    @Builder.Default
    private String createdAt = Instant.now().toString();

    @Builder.Default
    @JsonProperty("is_favorite")
    @JsonAlias("is_favorite")
    private List<String> isFavorite = new ArrayList<>();

    @Builder.Default
    @JsonProperty("is_muted")
    @JsonAlias("is_muted")
    private List<String> isMuted = new ArrayList<>();

    @Builder.Default
    private Map<String, Integer> unreadCount = new HashMap<>();
}
