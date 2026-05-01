package com.chathub.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class ChannelUpdateRequest {
    private String name;
    private String description;
    
    @JsonProperty("is_private")
    @JsonAlias("is_private")
    private Boolean isPrivate;

    @JsonProperty("password")
    private String password;
}
