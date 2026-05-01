package com.chathub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class MessageCreateRequest {
    @NotBlank
    @JsonProperty("channel_id")
    @JsonAlias({"channelId", "channel_id"})
    private String channelId;

    @NotBlank
    private String content;

    @JsonProperty("reply_to")
    @JsonAlias({"replyTo", "reply_to"})
    private String replyTo;

    @JsonProperty("thread_id")
    @JsonAlias({"threadId", "thread_id"})
    private String threadId;
}
