package com.nexa.auth;

import com.nexa.auth.dto.RecoveryCodesResponse;
import com.nexa.auth.dto.TwoFactorSetupResponse;
import com.nexa.common.ApiException;
import com.nexa.security.SecretCipher;
import com.nexa.security.Totp;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Kétlépcsős hitelesítés (TOTP) üzleti logikája (#17). A titok titkosítva ({@link SecretCipher})
 * kerül a felhasználóra, a QR-t a frontend rajzolja az {@code otpauth://} URI-ból. A bekapcsoláskor
 * egyszer-használatos helyreállító kódokat is generálunk (BCrypt-hash-elve tárolva).
 *
 * <p>Bejelentkezéskor a {@link #verify} fogadja el a TOTP-t <b>vagy</b> egy fel nem használt
 * helyreállító kódot (utóbbit beváltja). A 2FA alapból kikapcsolt, bármikor ki/be kapcsolható.
 */
@Service
public class TwoFactorService {

    private static final String ISSUER = "Nexa";
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // I/O/0/1 nélkül

    private final UserRepository userRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final SecretCipher secretCipher;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public TwoFactorService(UserRepository userRepository,
                            RecoveryCodeRepository recoveryCodeRepository,
                            SecretCipher secretCipher,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.secretCipher = secretCipher;
        this.passwordEncoder = passwordEncoder;
    }

    /** A beállítás indítása: ideiglenes (még inaktív) titok mentése + az otpauth URI a QR-hez. */
    @Transactional
    public TwoFactorSetupResponse beginSetup(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        if (user.isTotpEnabled()) {
            throw ApiException.twoFactorAlreadyEnabled();
        }
        String secret = Totp.generateSecretBase32();
        user.setTotpSecret(secretCipher.encrypt(secret));
        return new TwoFactorSetupResponse(secret, Totp.otpauthUri(ISSUER, user.getEmail(), secret));
    }

    /** A beállítás véglegesítése egy érvényes kóddal → 2FA bekapcsol + helyreállító kódok. */
    @Transactional
    public RecoveryCodesResponse enable(UUID userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        if (user.isTotpEnabled()) {
            throw ApiException.twoFactorAlreadyEnabled();
        }
        String encrypted = user.getTotpSecret();
        if (encrypted == null) {
            throw ApiException.twoFactorNotInSetup();
        }
        if (!Totp.verify(secretCipher.decrypt(encrypted), code)) {
            throw ApiException.invalid2faCode();
        }
        user.setTotpEnabled(true);
        return new RecoveryCodesResponse(regenerateRecoveryCodes(user));
    }

    /** A 2FA kikapcsolása egy érvényes (TOTP vagy helyreállító) kóddal. Ha nincs bekapcsolva: no-op. */
    @Transactional
    public void disable(UUID userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        if (!user.isTotpEnabled()) {
            return;
        }
        if (!verify(user, code)) {
            throw ApiException.invalid2faCode();
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        recoveryCodeRepository.deleteByUser(user);
    }

    /**
     * Egy bejelentkezési kód ellenőrzése: érvényes TOTP, VAGY egy fel nem használt helyreállító
     * kód (amit egyúttal beváltunk). A hívó tranzakciójában fut (a recovery kód beváltása perzisztál).
     */
    @Transactional
    public boolean verify(User user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String secret = secretCipher.decrypt(user.getTotpSecret());
        if (secret != null && Totp.verify(secret, code)) {
            return true;
        }
        String normalized = normalizeRecovery(code);
        for (RecoveryCode candidate : recoveryCodeRepository.findByUserIdAndUsedFalse(user.getId())) {
            if (passwordEncoder.matches(normalized, candidate.getCodeHash())) {
                candidate.markUsed();
                return true;
            }
        }
        return false;
    }

    private List<String> regenerateRecoveryCodes(User user) {
        recoveryCodeRepository.deleteByUser(user);
        List<String> raw = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = generateRecoveryCode();
            raw.add(code);
            recoveryCodeRepository.save(new RecoveryCode(user, passwordEncoder.encode(normalizeRecovery(code))));
        }
        return raw;
    }

    /** Olvasható, „XXXX-XXXX" formátumú kód (a hasonló karaktereket kihagyva). */
    private String generateRecoveryCode() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(RECOVERY_ALPHABET.charAt(random.nextInt(RECOVERY_ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Illesztés előtt egységesít: nagybetű + a kötőjelek/szóközök eltávolítása. */
    private String normalizeRecovery(String code) {
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
    }
}
