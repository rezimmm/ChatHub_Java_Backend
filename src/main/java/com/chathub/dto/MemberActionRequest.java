package com.chathub.dto;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class MemberActionRequest {
    @JsonProperty("user_id")
    @JsonAlias("user_id")
    private String userId;
}
