package com.chathub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UserUpdateRequest {
    @Size(min = 2, max = 30)
    private String username;
    private String status;
    private String bio;

    @JsonAlias({"avatar_url", "avatarUrl"})
    private String avatarUrl;

    @JsonAlias({"avatar_color", "avatarColor"})
    private String avatarColor;
}
