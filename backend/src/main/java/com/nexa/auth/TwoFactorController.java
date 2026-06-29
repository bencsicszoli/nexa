package com.nexa.auth;

import com.nexa.auth.dto.RecoveryCodesResponse;
import com.nexa.auth.dto.TwoFactorCodeRequest;
import com.nexa.auth.dto.TwoFactorSetupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Kétlépcsős hitelesítés (2FA/TOTP) végpontjai az {@code /api/auth/2fa} prefix alatt (#17) —
 * mind hitelesítést igényel (a bejelentkezett felhasználó kezeli a saját 2FA-ját). NEM gatelt.
 * A login-folyam ettől külön, publikus végpontokon megy ({@code /login}, {@code /login/2fa}).
 */
@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public TwoFactorController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    /** A beállítás indítása: titok + otpauth URI a QR-hez (a 2FA még nem aktív). */
    @PostMapping("/setup")
    public TwoFactorSetupResponse setup(@AuthenticationPrincipal UUID userId) {
        return twoFactorService.beginSetup(userId);
    }

    /** A beállítás véglegesítése egy érvényes kóddal → bekapcsol + helyreállító kódok (egyszer). */
    @PostMapping("/enable")
    public RecoveryCodesResponse enable(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TwoFactorCodeRequest request) {
        return twoFactorService.enable(userId, request.code());
    }

    /** A 2FA kikapcsolása egy érvényes (TOTP vagy helyreállító) kóddal. */
    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TwoFactorCodeRequest request) {
        twoFactorService.disable(userId, request.code());
    }
}
