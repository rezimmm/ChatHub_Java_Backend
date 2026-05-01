package com.chathub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "channel_timestamp", def = "{'channelId': 1, 'timestamp': -1}")
})
public class Message {

    @Id
    private String mongoId;

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @JsonProperty("channel_id")
    @JsonAlias("channel_id")
    private String channelId;
    
    @JsonProperty("user_id")
    @JsonAlias("user_id")
    private String userId;
    private String username;
    private String content;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    @Builder.Default
    @JsonProperty("avatar_color")
    @JsonAlias("avatar_color")
    private String avatarColor = "#7c3aed";

    @JsonProperty("avatar_url")
    @JsonAlias("avatar_url")
    private String avatarUrl;

    @Builder.Default
    private boolean edited = false;

    @JsonProperty("edited_at")
    @JsonAlias("edited_at")
    private String editedAt;

    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    @JsonProperty("file_url")
    @JsonAlias("file_url")
    private String fileUrl;
    
    @JsonProperty("file_name")
    @JsonAlias("file_name")
    private String fileName;
    
    @JsonProperty("file_type")
    @JsonAlias("file_type")
    private String fileType;

    @Builder.Default
    private boolean pinned = false;

    @JsonProperty("reply_to")
    @JsonAlias("reply_to")
    private String replyTo;

    @Builder.Default
    @JsonProperty("read_by")
    @JsonAlias("read_by")
    private List<String> readBy = new ArrayList<>();

    @JsonProperty("thread_id")
    @JsonAlias("thread_id")
    private String threadId;

    @Builder.Default
    @JsonProperty("reply_count")
    @JsonAlias("reply_count")
    private int replyCount = 0;
}
