package com.ainovel.app.user;

import com.ainovel.app.economy.EconomyService;
import com.ainovel.app.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EconomyService economyService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User currentUser(UserDetails details) {
        return userRepository.findByUsername(details.getUsername()).orElseThrow();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> profile(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        return ResponseEntity.ok(toProfile(user));
    }

    @PostMapping("/check-in")
    public ResponseEntity<CreditChangeResponse> checkIn(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        EconomyService.CreditChangeResult result = economyService.checkIn(user);
        return ResponseEntity.ok(new CreditChangeResponse(result.success(), result.points(), result.newTotal()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<CreditChangeResponse> redeem(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody RedeemRequest request) {
        User user = currentUser(principal);
        EconomyService.CreditChangeResult result = economyService.redeem(user, request.code());
        return ResponseEntity.ok(new CreditChangeResponse(result.success(), result.points(), result.newTotal()));
    }

    @PostMapping("/password")
    public ResponseEntity<BasicResponse> updatePassword(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody PasswordUpdateRequest request) {
        User user = currentUser(principal);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(new BasicResponse(false, "原密码不正确"));
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(new BasicResponse(true, "密码修改成功"));
    }

    private UserProfileResponse toProfile(User user) {
        String role = user.hasRole("ROLE_ADMIN") ? "admin" : "user";
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                role,
                user.getCredits(),
                user.isBanned(),
                user.getLastCheckInAt()
        );
    }
}

