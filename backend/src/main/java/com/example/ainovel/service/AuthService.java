package com.example.ainovel.service;

import com.example.ainovel.dto.RegisterRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for authentication-related operations like user registration and login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Registers a new user based on the provided registration data.
     *
     * @param registerRequest DTO containing user registration information.
     * @return The created User entity.
     * @throws IllegalArgumentException if the username or email is already taken.
     */
    @Transactional
    public User register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        return userRepository.save(newUser);
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @return A JWT token string.
     */
    public String login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtil.generateToken(userDetails);
    }
}
