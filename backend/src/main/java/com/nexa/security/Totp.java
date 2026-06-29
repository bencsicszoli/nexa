package com.nexa.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Időalapú egyszer-használatos jelszó (TOTP, RFC 6238) saját megvalósítása — nulla új függőség (#17).
 * HmacSHA1, 30 másodperces időablak, 6 jegyű kód; a verifikáció ±1 ablakot is elfogad (óracsúszás).
 * A titok Base32-ben (RFC 4648) áll, ahogy az authenticator alkalmazások várják.
 */
public final class Totp {

    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int SECRET_BYTES = 20; // 160 bit — a szokásos TOTP-titokméret
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {
    }

    /** Új véletlen TOTP-titok Base32-ben (padding nélkül). */
    public static String generateSecretBase32() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** A megadott titokhoz tartozó aktuális 6 jegyű kód (a mostani időablakra). */
    public static String currentCode(String base32Secret) {
        return codeForTimeStep(base32Secret, System.currentTimeMillis() / 1000L / PERIOD_SECONDS);
    }

    /**
     * Igaz, ha a kód a mostani vagy a szomszédos (±1) időablakra érvényes. A nem-számjegy
     * vagy rossz hosszú bemenetet csendben elutasítja.
     */
    public static boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null) {
            return false;
        }
        String normalized = code.trim();
        if (!normalized.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long timeStep = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        for (int offset = -1; offset <= 1; offset++) {
            if (constantTimeEquals(normalized, codeForTimeStep(base32Secret, timeStep + offset))) {
                return true;
            }
        }
        return false;
    }

    /** Az otpauth:// URI az authenticator-alkalmazás QR-kódjához. */
    public static String otpauthUri(String issuer, String accountName, String base32Secret) {
        String label = urlEncode(issuer) + ":" + urlEncode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    private static String codeForTimeStep(String base32Secret, long timeStep) {
        byte[] key = base32Decode(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP computation failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    // --- Base32 (RFC 4648, padding nélkül) ---

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String base32) {
        String cleaned = base32.trim().replace("=", "").toUpperCase(Locale.ROOT);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : cleaned.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(c);
            if (index < 0) {
                continue; // nem-Base32 karakter (pl. szóköz) átugorva
            }
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
