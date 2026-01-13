package com.ainovel.app.user;

import com.ainovel.app.economy.EconomyService;
import com.ainovel.app.manuscript.model.Manuscript;
import com.ainovel.app.manuscript.repo.ManuscriptRepository;
import com.ainovel.app.story.model.Outline;
import com.ainovel.app.story.model.Story;
import com.ainovel.app.story.repo.OutlineRepository;
import com.ainovel.app.story.repo.StoryRepository;
import com.ainovel.app.user.dto.*;
import com.ainovel.app.world.model.World;
import com.ainovel.app.world.repo.WorldRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/v1/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EconomyService economyService;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private WorldRepository worldRepository;
    @Autowired
    private OutlineRepository outlineRepository;
    @Autowired
    private ManuscriptRepository manuscriptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User currentUser(UserDetails details) {
        return userRepository.findByUsername(details.getUsername()).orElseThrow();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> profile(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        return ResponseEntity.ok(toProfile(user));
    }

    @GetMapping("/summary")
    public ResponseEntity<UserSummaryResponse> summary(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        long novelCount = storyRepository.countByUser(user);
        long worldCount = worldRepository.countByUser(user);
        long totalWords = estimateTotalWords(user);
        long totalEntries = estimateWorldEntries(user);
        return ResponseEntity.ok(new UserSummaryResponse(novelCount, worldCount, totalWords, totalEntries));
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
    public ResponseEntity<BasicResponse> updatePassword(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(501).body(new BasicResponse(false, "PASSWORD_MANAGED_BY_SSO"));
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

    private long estimateTotalWords(User user) {
        long total = 0;
        for (Story story : storyRepository.findByUser(user)) {
            for (Outline outline : outlineRepository.findByStory(story)) {
                for (Manuscript manuscript : manuscriptRepository.findByOutline(outline)) {
                    total += estimateWordsFromSections(manuscript.getSectionsJson());
                }
            }
        }
        return total;
    }

    private long estimateWordsFromSections(String sectionsJson) {
        if (sectionsJson == null || sectionsJson.isBlank()) return 0;
        try {
            Map<String, String> sections = objectMapper.readValue(sectionsJson, new TypeReference<>() {});
            long total = 0;
            for (String html : sections.values()) {
                if (html == null) continue;
                String plain = html.replaceAll("<[^>]*>", "");
                total += plain.trim().length();
            }
            return total;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long estimateWorldEntries(User user) {
        long total = 0;
        for (World world : worldRepository.findByUser(user)) {
            total += countNonEmptyEntries(world.getModulesJson());
        }
        return total;
    }

    private long countNonEmptyEntries(String modulesJson) {
        if (modulesJson == null || modulesJson.isBlank()) return 0;
        try {
            Map<String, Map<String, String>> modules = objectMapper.readValue(modulesJson, new TypeReference<>() {});
            long total = 0;
            for (Map<String, String> fields : modules.values()) {
                if (fields == null) continue;
                for (String v : fields.values()) {
                    if (v != null && !v.isBlank()) total++;
                }
            }
            return total;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
