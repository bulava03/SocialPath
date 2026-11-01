package com.socialpath.service.impl;

import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;
import com.socialpath.repository.PhoneCodeRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.sms.SmsGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PhoneVerificationServiceImpl implements PhoneVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final SmsGateway smsGateway;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    public PhoneCode requestCode(User user) {
        if (user.getPhone() == null) {
            throw new IllegalStateException("No phone number to confirm for " + user.getLogin());
        }
        PhoneCode code = codeRepository.findByUserLogin(user.getLogin())
                .orElseGet(() -> new PhoneCode(user.getLogin()));
        if (user.getPhone().equals(code.getPhone()) && isUsable(code) && secondsUntilResend(code) > 0) {
            return code;
        }

        String digits = "%06d".formatted(RANDOM.nextInt(1_000_000));
        LocalDateTime now = now();
        code.setPhone(user.getPhone());
        code.setCodeHash(passwordEncoder.encode(digits));
        code.setAttempts(0);
        code.setSentAt(now);
        code.setExpiresAt(now.plus(CODE_LIFETIME));
        PhoneCode saved = codeRepository.save(code);
        smsGateway.send(user.getPhone(), "SocialPath code: " + digits
                + ". It expires in " + CODE_LIFETIME.toMinutes() + " minutes. Do not share it with anyone.");
        return saved;
    }

    @Override
    public Optional<PhoneCode> find(String id) {
        return id == null ? Optional.empty() : codeRepository.findById(id);
    }

    @Override
    public Optional<PhoneCode> findFor(String login) {
        return codeRepository.findByUserLogin(login);
    }

    @Override
    public Outcome confirm(PhoneCode code, String typed) {
        User user = userRepository.findByLogin(code.getUserLogin());
        // A code confirms the number it went to, not one the profile changed to since.
        if (user == null || !code.getPhone().equals(user.getPhone()) || !isUsable(code)) {
            return Outcome.EXPIRED;
        }
        String digits = typed == null ? "" : typed.replaceAll("\\s", "");
        if (!passwordEncoder.matches(digits, code.getCodeHash())) {
            code.setAttempts(code.getAttempts() + 1);
            codeRepository.save(code);
            return isUsable(code) ? Outcome.WRONG_CODE : Outcome.EXPIRED;
        }
        if (userRepository.isPhoneVerifiedByOther(user.getPhone(), user.getLogin())) {
            return Outcome.NUMBER_TAKEN;
        }

        user.setPhoneVerified(true);
        user.setPendingSince(null);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two accounts confirming one number at the same moment: the unique key lets one through.
            return Outcome.NUMBER_TAKEN;
        }
        codeRepository.delete(code);
        return Outcome.CONFIRMED;
    }

    @Override
    public long secondsUntilResend(PhoneCode code) {
        if (code.getSentAt() == null) {
            return 0;
        }
        long seconds = Duration.between(now(), code.getSentAt().plus(RESEND_AFTER)).toSeconds();
        return Math.max(0, seconds);
    }

    @Override
    public void cancelRegistration(PhoneCode code) {
        User user = userRepository.findByLogin(code.getUserLogin());
        if (user != null && user.getPendingSince() != null) {
            // The code row goes with it (ON DELETE CASCADE).
            userRepository.delete(user);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT2M")
    public void removeAbandoned() {
        LocalDateTime cutoff = now().minus(PENDING_LIFETIME);
        userRepository.deletePendingBefore(cutoff);
        codeRepository.deleteExpiredBefore(cutoff);
    }

    private boolean isUsable(PhoneCode code) {
        return code.getExpiresAt() != null && code.getExpiresAt().isAfter(now())
                && code.getAttempts() < MAX_ATTEMPTS;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
