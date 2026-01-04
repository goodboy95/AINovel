package com.ainovel.app.economy;

import com.ainovel.app.economy.model.CreditLog;
import com.ainovel.app.economy.model.RedeemCode;
import com.ainovel.app.economy.repo.CreditLogRepository;
import com.ainovel.app.economy.repo.RedeemCodeRepository;
import com.ainovel.app.settings.model.GlobalSettings;
import com.ainovel.app.settings.repo.GlobalSettingsRepository;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class EconomyService {
    private static final ZoneId CHECK_IN_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private CreditLogRepository creditLogRepository;
    @Autowired
    private RedeemCodeRepository redeemCodeRepository;
    @Autowired
    private GlobalSettingsRepository globalSettingsRepository;
    @Autowired
    private UserRepository userRepository;

    public record CreditChangeResult(boolean success, double points, double newTotal) {}

    @Transactional
    public CreditChangeResult checkIn(User user) {
        if (hasCheckedInToday(user)) {
            return new CreditChangeResult(false, 0, user.getCredits());
        }

        GlobalSettings global = globalSettingsRepository.findTopByOrderByUpdatedAtDesc().orElseGet(GlobalSettings::new);
        int min = Math.max(0, global.getCheckInMinPoints());
        int max = Math.max(min, global.getCheckInMaxPoints());
        int points = min + (int) Math.floor(Math.random() * (max - min + 1));

        user.setCredits(user.getCredits() + points);
        user.setLastCheckInAt(Instant.now());
        userRepository.save(user);
        log(user, points, "check_in", "每日签到");

        return new CreditChangeResult(true, points, user.getCredits());
    }

    @Transactional
    public CreditChangeResult redeem(User user, String code) {
        RedeemCode redeem = redeemCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("兑换码不存在"));
        if (redeem.isUsed()) {
            throw new RuntimeException("兑换码已使用");
        }
        if (redeem.getExpiresAt() != null && redeem.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("兑换码已过期");
        }

        redeem.setUsed(true);
        redeem.setUsedBy(user);
        redeem.setUsedAt(Instant.now());
        redeemCodeRepository.save(redeem);

        user.setCredits(user.getCredits() + redeem.getAmount());
        userRepository.save(user);
        log(user, redeem.getAmount(), "redeem", "兑换码: " + redeem.getCode());

        return new CreditChangeResult(true, redeem.getAmount(), user.getCredits());
    }

    @Transactional
    public void grant(User user, double amount, String details) {
        user.setCredits(user.getCredits() + amount);
        userRepository.save(user);
        log(user, amount, "admin_grant", details == null ? "管理员手动调整" : details);
    }

    @Transactional
    public void deduct(User user, double amount, String details) {
        user.setCredits(user.getCredits() - amount);
        userRepository.save(user);
        log(user, -amount, "generation", details);
    }

    private void log(User user, double amount, String reason, String details) {
        CreditLog log = new CreditLog();
        log.setUser(user);
        log.setAmount(amount);
        log.setReason(reason);
        log.setDetails(details);
        creditLogRepository.save(log);
    }

    public boolean hasCheckedInToday(User user) {
        if (user.getLastCheckInAt() == null) return false;
        LocalDate last = user.getLastCheckInAt().atZone(CHECK_IN_ZONE).toLocalDate();
        LocalDate today = Instant.now().atZone(CHECK_IN_ZONE).toLocalDate();
        return today.equals(last);
    }

    public Optional<GlobalSettings> globalSettings() {
        return globalSettingsRepository.findTopByOrderByUpdatedAtDesc();
    }
}
