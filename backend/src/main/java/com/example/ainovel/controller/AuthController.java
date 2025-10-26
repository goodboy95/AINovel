package com.example.ainovel.controller;

import com.example.ainovel.dto.LoginRequest;
import com.example.ainovel.dto.RegisterRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.security.PermissionLevel;
import com.example.ainovel.service.AuthService;
import com.example.ainovel.service.security.PermissionService;
import com.example.ainovel.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling user authentication operations, such as registration and login.
 */
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final AuthService authService;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;

    /**
     * Constructs an AuthController with the necessary AuthService.
     * @param authService The service for handling authentication logic.
     */
    public AuthController(AuthService authService,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          PermissionService permissionService) {
        this.authService = authService;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.permissionService = permissionService;
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

    /**
     * 验证 JWT Token 的有效性。
     * 路径: GET /api/auth/validate
     * 请求头: Authorization: Bearer <token>
     * 成功时返回 200，并包含用户名；失败返回 401。
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Boolean valid = jwtUtil.validateToken(token, userDetails);
            if (Boolean.TRUE.equals(valid)) {
                Long userId = extractUserId(userDetails);
                Map<String, List<String>> permissions = userId != null
                    ? toResponse(permissionService.findPermissionsForWorkspace(userId, userId))
                    : Collections.emptyMap();
                return ResponseEntity.ok(Map.of(
                    "username", username,
                    "userId", userId,
                    "workspaceId", userId,
                    "permissions", permissions
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private Map<String, List<String>> toResponse(Map<String, java.util.Set<PermissionLevel>> permissions) {
        return permissions.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(PermissionLevel::name)
                    .sorted()
                    .toList()));
    }
}
