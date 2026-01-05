package com.ainovel.app.user;

import com.ainovel.app.user.model.EmailVerificationCode;
import com.ainovel.app.user.repo.EmailVerificationCodeRepository;
import com.ainovel.app.settings.SettingsService;
import com.ainovel.app.settings.model.GlobalSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;

@Service
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final int RATE_LIMIT_PER_WINDOW = 5;
    private static final Random RANDOM = new Random();

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private SettingsService settingsService;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public boolean isRateLimited(String email, String purpose) {
        Instant since = Instant.now().minus(RATE_WINDOW);
        long sent = emailVerificationCodeRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(email, purpose, since);
        return sent >= RATE_LIMIT_PER_WINDOW;
    }

    @Transactional
    public EmailVerificationCode sendRegistrationCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerificationCode record = new EmailVerificationCode();
        record.setEmail(email);
        record.setCode(code);
        record.setPurpose("register");
        record.setExpiresAt(Instant.now().plus(CODE_TTL));
        emailVerificationCodeRepository.save(record);

        SimpleMailMessage message = new SimpleMailMessage();
        JavaMailSender sender = resolveMailSender();
        String configuredFrom = resolveFromAddress();
        if (configuredFrom != null && !configuredFrom.isBlank()) {
            message.setFrom(configuredFrom);
        }
        message.setTo(email);
        message.setSubject("AiNovel 注册验证码");
        message.setText("你的注册验证码是: " + code + "\n\n有效期 10 分钟，如非本人操作请忽略。");
        sender.send(message);

        return record;
    }

    @Transactional
    public EmailVerificationCode verifyRegistrationCode(String email, String code) {
        EmailVerificationCode record = emailVerificationCodeRepository
                .findFirstByEmailIgnoreCaseAndPurposeAndCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, "register", code, Instant.now()
                )
                .orElseThrow(() -> new RuntimeException("验证码无效或已过期"));
        record.setUsed(true);
        record.setUsedAt(Instant.now());
        emailVerificationCodeRepository.save(record);
        return record;
    }

    public void sendTestEmail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        JavaMailSender sender = resolveMailSender();
        String configuredFrom = resolveFromAddress();
        if (configuredFrom != null && !configuredFrom.isBlank()) {
            message.setFrom(configuredFrom);
        }
        message.setTo(email);
        message.setSubject("AiNovel SMTP 测试邮件");
        message.setText("这是一封测试邮件，说明 SMTP 配置可用。");
        sender.send(message);
    }

    private String resolveFromAddress() {
        GlobalSettings g = settingsService.getGlobalSettings();
        if (g.getSmtpUsername() != null && !g.getSmtpUsername().isBlank()) return g.getSmtpUsername();
        return fromAddress;
    }

    private JavaMailSender resolveMailSender() {
        GlobalSettings g = settingsService.getGlobalSettings();
        if (g.getSmtpHost() == null || g.getSmtpHost().isBlank()) {
            return mailSender;
        }
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(g.getSmtpHost());
        impl.setPort(g.getSmtpPort() != null ? g.getSmtpPort() : 587);
        impl.setUsername(g.getSmtpUsername());
        impl.setPassword(g.getSmtpPassword());
        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return impl;
    }
}
