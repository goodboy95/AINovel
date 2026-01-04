package com.ainovel.app.user;

import com.ainovel.app.user.model.EmailVerificationCode;
import com.ainovel.app.user.repo.EmailVerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Service
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Random RANDOM = new Random();

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

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
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(email);
        message.setSubject("AiNovel 注册验证码");
        message.setText("你的注册验证码是: " + code + "\n\n有效期 10 分钟，如非本人操作请忽略。");
        mailSender.send(message);

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
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(email);
        message.setSubject("AiNovel SMTP 测试邮件");
        message.setText("这是一封测试邮件，说明 SMTP 配置可用。");
        mailSender.send(message);
    }
}

