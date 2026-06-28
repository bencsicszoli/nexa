package com.nexa.subscription;

import com.nexa.common.ApiException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * A Paddle webhookok hitelességét ellenőrzi. A {@code Paddle-Signature} fejléc
 * formátuma {@code ts=<unix>;h1=<hex>}, ahol a {@code h1} a {@code "<ts>:<rawBody>"}
 * sztring HMAC-SHA256 lenyomata a webhook-célpont titkos kulcsával.
 * Az aláírás-mintát a {@code LocalStorageService} HMAC-megoldása ihlette.
 */
@Component
public class PaddleWebhookVerifier {

    private final PaddleProperties properties;

    public PaddleWebhookVerifier(PaddleProperties properties) {
        this.properties = properties;
    }

    /**
     * Ellenőrzi az aláírást a nyers törzs ellen. Hibás/hiányzó/lejárt aláírásnál
     * {@link ApiException#invalidWebhookSignature()} dobódik.
     */
    public void verify(String rawBody, String signatureHeader) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank() || signatureHeader == null || rawBody == null) {
            throw ApiException.invalidWebhookSignature();
        }

        String ts = null;
        String h1 = null;
        for (String part : signatureHeader.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            if ("ts".equals(key)) {
                ts = value;
            } else if ("h1".equals(key)) {
                h1 = value;
            }
        }
        if (ts == null || h1 == null) {
            throw ApiException.invalidWebhookSignature();
        }

        // Replay elleni védelem: a túl régi (vagy jövőbeli) bélyeget elutasítjuk.
        try {
            long tsSeconds = Long.parseLong(ts);
            long skew = Math.abs(Instant.now().getEpochSecond() - tsSeconds);
            if (skew > properties.getWebhookToleranceSeconds()) {
                throw ApiException.invalidWebhookSignature();
            }
        } catch (NumberFormatException e) {
            throw ApiException.invalidWebhookSignature();
        }

        byte[] expected = hmac(secret, ts + ":" + rawBody);
        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(h1);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidWebhookSignature();
        }
        if (!MessageDigest.isEqual(expected, provided)) {
            throw ApiException.invalidWebhookSignature();
        }
    }

    /** Segéd a tesztekhez/aláíráshoz: a {@code "<ts>:<body>"} HMAC-SHA256 hex lenyomata. */
    public String sign(String ts, String rawBody) {
        return HexFormat.of().formatHex(hmac(properties.getWebhookSecret(), ts + ":" + rawBody));
    }

    private static byte[] hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 nem elérhető", e);
        }
    }
}
