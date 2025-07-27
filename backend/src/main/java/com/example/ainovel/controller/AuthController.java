package com.example.ainovel.controller;

import com.example.ainovel.dto.LoginRequest;
import com.example.ainovel.dto.RegisterRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling user authentication operations, such as registration and login.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructs an AuthController with the necessary AuthService.
     * @param authService The service for handling authentication logic.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user in the system.
     * @param registerRequest The request object containing user registration details.
     * @return A ResponseEntity indicating the result of the registration attempt.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Authenticates a user and returns a JWT token upon successful login.
     * @param loginRequest The request object containing user login credentials.
     * @return A ResponseEntity containing the JWT token or an error message.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            // It's generally better to return a more generic error message for security reasons.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }
    }
}
