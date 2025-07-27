package com.example.ainovel.dto;

import lombok.Data;

/**
 * Data Transfer Object for user login requests.
 */
@Data
public class LoginRequest {
    /**
     * The user's username.
     */
    private String username;

    /**
     * The user's password.
     */
    private String password;
}
