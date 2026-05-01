package com.chathub.dto;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class InviteCreateRequest {
    @JsonProperty("expires_in_hours")
    @JsonAlias("expires_in_hours")
    private Integer expiresInHours;

    @JsonProperty("expires_in_minutes")
    @JsonAlias("expires_in_minutes")
    private Integer expiresInMinutes;

    @JsonProperty("max_uses")
    @JsonAlias("max_uses")
    private Integer maxUses;
}
