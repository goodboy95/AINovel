package com.ainovel.app.user;

import com.ainovel.app.security.JwtService;
import com.ainovel.app.user.dto.AuthResponse;
import com.ainovel.app.user.dto.RegisterRequest;
import com.ainovel.app.user.dto.RegisterV2Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(u -> { throw new RuntimeException("用户名已存在"); });
        userRepository.findByEmail(request.email()).ifPresent(u -> { throw new RuntimeException("邮箱已被注册"); });
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of("ROLE_USER"));
        user.setCredits(500.0);
        userRepository.save(user);
        String token = generateToken(user);
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }

    @Transactional
    public void registerV2(RegisterV2Request request) {
        emailVerificationService.verifyRegistrationCode(request.email(), request.code());
        register(new RegisterRequest(request.username(), request.email(), request.password()));
    }

    public AuthResponse login(String username, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        User user = userRepository.findByUsername(username).orElseThrow();
        String token = generateToken(user);
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }

    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        return jwtService.generateToken(user.getUsername(), claims);
    }
}
