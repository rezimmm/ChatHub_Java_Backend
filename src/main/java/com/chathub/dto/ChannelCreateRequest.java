package com.chathub.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class ChannelCreateRequest {
    @NotBlank
    private String name;
    private String description = "";
    
    @JsonProperty("is_dm")
    @JsonAlias("is_dm")
    private Boolean isDm = false;
    
    @JsonProperty("is_private")
    @JsonAlias("is_private")
    private Boolean isPrivate = false;

    @JsonProperty("password")
    private String password;
    
    private List<String> members;
}
