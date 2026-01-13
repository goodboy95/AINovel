package com.ainovel.app.admin;

import com.ainovel.app.admin.dto.*;
import com.ainovel.app.ai.model.ModelConfigEntity;
import com.ainovel.app.ai.repo.ModelConfigRepository;
import com.ainovel.app.economy.EconomyService;
import com.ainovel.app.economy.model.RedeemCode;
import com.ainovel.app.economy.repo.CreditLogRepository;
import com.ainovel.app.economy.repo.RedeemCodeRepository;
import com.ainovel.app.material.repo.MaterialRepository;
import com.ainovel.app.settings.SettingsService;
import com.ainovel.app.settings.SmtpService;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import com.ainovel.app.settings.model.GlobalSettings;
import com.ainovel.app.settings.repo.GlobalSettingsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CreditLogRepository creditLogRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private ModelConfigRepository modelConfigRepository;
    @Autowired
    private RedeemCodeRepository redeemCodeRepository;
    @Autowired
    private EconomyService economyService;
    @Autowired
    private SmtpService smtpService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private GlobalSettingsRepository globalSettingsRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStatsResponse> dashboard() {
        Instant todayStart = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        long totalUsers = userRepository.count();
        long todayNewUsers = userRepository.countByCreatedAtAfter(todayStart);
        double totalConsumed = creditLogRepository.totalConsumed();
        double todayConsumed = creditLogRepository.consumedSince(todayStart);
        long pendingReviews = materialRepository.countByStatusIgnoreCase("pending");
        double apiErrorRate = 0.0;
        return ResponseEntity.ok(new AdminDashboardStatsResponse(totalUsers, todayNewUsers, totalConsumed, todayConsumed, apiErrorRate, pendingReviews));
    }

    @GetMapping("/models")
    public List<ModelConfigDto> models() {
        return modelConfigRepository.findAll().stream().map(this::toModelDto).toList();
    }

    @PutMapping("/models/{id}")
    public ResponseEntity<Boolean> updateModel(@PathVariable UUID id, @RequestBody ModelConfigDto dto) {
        ModelConfigEntity entity = modelConfigRepository.findById(id).orElseThrow(() -> new RuntimeException("模型不存在"));
        if (dto.displayName() != null) entity.setDisplayName(dto.displayName());
        if (dto.name() != null) entity.setName(dto.name());
        entity.setInputMultiplier(dto.inputMultiplier());
        entity.setOutputMultiplier(dto.outputMultiplier());
        if (dto.poolId() != null) entity.setPoolId(dto.poolId());
        entity.setEnabled(dto.isEnabled());
        modelConfigRepository.save(entity);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return userRepository.findAll().stream().map(this::toUserDto).toList();
    }

    @PostMapping("/users/{id}/grant-credits")
    public ResponseEntity<Boolean> grantCredits(@PathVariable UUID id, @RequestBody GrantCreditsRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        economyService.grant(user, request.amount(), "管理员手动调整");
        return ResponseEntity.ok(true);
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<Boolean> ban(@PathVariable UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setBanned(true);
        userRepository.save(user);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<Boolean> unban(@PathVariable UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setBanned(false);
        userRepository.save(user);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/logs")
    public List<CreditLogDto> logs(@RequestParam(defaultValue = "200") int limit) {
        return creditLogRepository.findAll(PageRequest.of(0, Math.min(500, Math.max(1, limit)), Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(l -> new CreditLogDto(l.getId(), l.getUser().getId(), l.getAmount(), l.getReason(), l.getDetails(), l.getCreatedAt()))
                .toList();
    }

    @GetMapping("/redeem-codes")
    public List<RedeemCodeDto> codes() {
        return redeemCodeRepository.findAll().stream()
                .map(c -> new RedeemCodeDto(c.getId(), c.getCode(), c.getAmount(), c.isUsed(), c.getUsedBy() == null ? null : c.getUsedBy().getUsername(), c.getExpiresAt()))
                .toList();
    }

    @PostMapping("/redeem-codes")
    public ResponseEntity<Boolean> createCode(@Valid @RequestBody CreateRedeemCodeRequest request) {
        RedeemCode code = new RedeemCode();
        code.setCode(request.code());
        code.setAmount(request.amount());
        code.setUsed(false);
        code.setExpiresAt(Instant.now().plusSeconds(3600L * 24 * 365));
        redeemCodeRepository.save(code);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/email/smtp")
    public SmtpStatusResponse smtpStatus() {
        var g = settingsService.getGlobalSettings();
        String host = g.getSmtpHost();
        Integer port = g.getSmtpPort();
        String username = g.getSmtpUsername();
        boolean pwd = g.getSmtpPassword() != null && !g.getSmtpPassword().isBlank();
        return new SmtpStatusResponse(host, port, username, pwd);
    }

    @GetMapping("/system-config")
    public AdminSystemConfigResponse systemConfig() {
        GlobalSettings g = settingsService.getGlobalSettings();
        return new AdminSystemConfigResponse(
                g.isRegistrationEnabled(),
                g.isMaintenanceMode(),
                g.getCheckInMinPoints(),
                g.getCheckInMaxPoints(),
                g.getSmtpHost(),
                g.getSmtpPort(),
                g.getSmtpUsername(),
                g.getSmtpPassword() != null && !g.getSmtpPassword().isBlank(),
                g.getLlmBaseUrl(),
                g.getLlmModelName(),
                g.getLlmApiKeyEncrypted() != null && !g.getLlmApiKeyEncrypted().isBlank()
        );
    }

    @PutMapping("/system-config")
    public AdminSystemConfigResponse updateSystemConfig(@RequestBody AdminSystemConfigUpdateRequest request) {
        GlobalSettings g = settingsService.getGlobalSettings();
        if (request.registrationEnabled() != null) g.setRegistrationEnabled(request.registrationEnabled());
        if (request.maintenanceMode() != null) g.setMaintenanceMode(request.maintenanceMode());
        if (request.checkInMinPoints() != null) g.setCheckInMinPoints(request.checkInMinPoints());
        if (request.checkInMaxPoints() != null) g.setCheckInMaxPoints(request.checkInMaxPoints());

        if (request.smtpHost() != null) g.setSmtpHost(request.smtpHost());
        if (request.smtpPort() != null) g.setSmtpPort(request.smtpPort());
        if (request.smtpUsername() != null) g.setSmtpUsername(request.smtpUsername());
        if (request.smtpPassword() != null && !request.smtpPassword().isBlank()) g.setSmtpPassword(request.smtpPassword());

        if (request.llmBaseUrl() != null) g.setLlmBaseUrl(request.llmBaseUrl());
        if (request.llmModelName() != null) g.setLlmModelName(request.llmModelName());
        if (request.llmApiKey() != null && !request.llmApiKey().isBlank()) g.setLlmApiKeyEncrypted(request.llmApiKey());

        globalSettingsRepository.save(g);
        return systemConfig();
    }

    @PostMapping("/email/test")
    public ResponseEntity<Boolean> testEmail(@Valid @RequestBody TestEmailRequest request) {
        smtpService.sendTestEmail(request.email());
        return ResponseEntity.ok(true);
    }

    private ModelConfigDto toModelDto(ModelConfigEntity entity) {
        return new ModelConfigDto(entity.getId(), entity.getName(), entity.getDisplayName(), entity.getInputMultiplier(), entity.getOutputMultiplier(), entity.getPoolId(), entity.isEnabled());
    }

    private AdminUserDto toUserDto(User user) {
        return new AdminUserDto(user.getId(), user.getUsername(), user.getEmail(), user.hasRole("ROLE_ADMIN") ? "admin" : "user", user.getCredits(), user.isBanned(), user.getLastCheckInAt());
    }
}
