package com.ainovel.app.user;

import com.ainovel.app.user.dto.AuthResponse;
import com.ainovel.app.user.dto.BasicResponse;
import com.ainovel.app.user.dto.LoginRequest;
import com.ainovel.app.user.dto.RegisterRequest;
import com.ainovel.app.user.dto.RegisterV2Request;
import com.ainovel.app.user.dto.SendCodeRequest;
import com.ainovel.app.user.dto.SendCodeResponse;
import com.ainovel.app.user.dto.ValidateResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ainovel.app.settings.SettingsService;
import com.ainovel.app.security.PowService;

@RestController
@RequestMapping({"/v1/auth", "/auth"})
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PowService powService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/send-code")
    public ResponseEntity<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        if (!settingsService.getGlobalSettings().isRegistrationEnabled()) {
            return ResponseEntity.ok(new SendCodeResponse(false, "当前未开放注册"));
        }
        if (!powService.verify(request.email(), request.captchaToken())) {
            return ResponseEntity.badRequest().body(new SendCodeResponse(false, "人机验证失败或已过期"));
        }
        if (emailVerificationService.isRateLimited(request.email(), "register")) {
            return ResponseEntity.status(429).body(new SendCodeResponse(false, "发送过于频繁，请稍后再试"));
        }
        emailVerificationService.sendRegistrationCode(request.email());
        return ResponseEntity.ok(new SendCodeResponse(true, "验证码已发送"));
    }

    @PostMapping("/register-v2")
    public ResponseEntity<BasicResponse> registerV2(@Valid @RequestBody RegisterV2Request request) {
        if (!settingsService.getGlobalSettings().isRegistrationEnabled()) {
            return ResponseEntity.ok(new BasicResponse(false, "当前未开放注册"));
        }
        authService.registerV2(request);
        return ResponseEntity.ok(new BasicResponse(true, "注册成功"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(new ValidateResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles()));
    }
}
