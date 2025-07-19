package com.ainovel.app.user;

import com.ainovel.app.user.dto.AuthResponse;
import com.ainovel.app.user.dto.LoginRequest;
import com.ainovel.app.user.dto.RegisterRequest;
import com.ainovel.app.user.dto.ValidateResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/v1/auth", "/auth"})
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()))
;    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(new ValidateResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles()));
    }
}
