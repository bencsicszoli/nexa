package com.nexa.storage;

import com.nexa.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Alapértelmezett (helyi fejlesztői) tároló: a feltöltés egy HMAC-csel aláírt,
 * rövid életű {@code /api/storage/upload} linkre megy, a bájtok a lemezre kerülnek,
 * a kiszolgálás a {@code /api/media} alól történik. Külső infrastruktúra nélkül,
 * azonnal tesztelhető a böngészőben. Éles/közös környezetben az {@code s3} provider
 * (R2 presigned URL) lép a helyére — a frontend-szerződés azonos.
 */
@Service
@ConditionalOnProperty(name = "nexa.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    // Ismert MIME-típusok → fájlkiterjesztés. A profil avatarhoz csak képek
    // jutnak el (a validáció a ProfileService-ben van); a videó a #6 kártyához kell.
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/x-matroska", ".mkv");

    private final Path root;
    private final byte[] signingKey;
    private final long uploadTtlSeconds;
    private final long maxUploadBytes;
    private final long maxVideoBytes;

    public LocalStorageService(
            @Value("${nexa.storage.local.dir}") String dir,
            @Value("${nexa.storage.local.secret}") String secret,
            @Value("${nexa.storage.local.upload-ttl-seconds:300}") long uploadTtlSeconds,
            @Value("${nexa.storage.max-upload-bytes:5242880}") long maxUploadBytes,
            @Value("${nexa.storage.max-video-bytes:52428800}") long maxVideoBytes) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.uploadTtlSeconds = uploadTtlSeconds;
        this.maxUploadBytes = maxUploadBytes;
        this.maxVideoBytes = maxVideoBytes;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Nem hozható létre a tárolómappa: " + root, e);
        }
    }

    @Override
    public PresignedUpload createUpload(String keyPrefix, String contentType) {
        String key = keyPrefix + "/" + UUID.randomUUID() + extensionFor(contentType);
        long expiresAt = Instant.now().getEpochSecond() + uploadTtlSeconds;
        String token = sign(key, contentType, expiresAt);
        return new PresignedUpload("/api/storage/upload?token=" + token, key);
    }

    @Override
    public String publicUrl(String key) {
        return "/api/media/" + key;
    }

    /**
     * Az aláírt feltöltési token beváltása: ellenőrzés → bájtok a lemezre.
     * A {@code requestContentType}-nek egyeznie kell az aláírt típussal.
     * Hívja a {@link StorageController}.
     */
    public void store(String token, String requestContentType, byte[] body) {
        SignedTarget target = verify(token);
        if (Instant.now().getEpochSecond() > target.expiresAt()) {
            throw ApiException.invalidUpload();
        }
        if (requestContentType != null && !target.contentType().equalsIgnoreCase(stripCharset(requestContentType))) {
            throw ApiException.invalidUpload();
        }
        if (body == null || body.length == 0) {
            throw ApiException.invalidUpload();
        }
        // Videóhoz nagyobb felső korlát, mint képhez (lásd nexa.storage.max-video-bytes).
        long limit = target.contentType().startsWith("video/") ? maxVideoBytes : maxUploadBytes;
        if (body.length > limit) {
            throw ApiException.payloadTooLarge();
        }
        Path dest = resolveWithinRoot(target.key());
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, body);
        } catch (IOException e) {
            throw new UncheckedIOException("Nem sikerült menteni a feltöltést: " + target.key(), e);
        }
    }

    /** Egy létező objektum elérési útja kiszolgáláshoz, vagy {@code null}, ha nincs. */
    public Path resolveForRead(String key) {
        Path file = resolveWithinRoot(key);
        return Files.isRegularFile(file) ? file : null;
    }

    /** A kiterjesztésből kikövetkeztetett tartalomtípus a kiszolgáláshoz. */
    public String contentTypeOf(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        for (Map.Entry<String, String> e : EXTENSIONS.entrySet()) {
            if (name.endsWith(e.getValue())) {
                return e.getKey();
            }
        }
        try {
            String probed = Files.probeContentType(file);
            return probed != null ? probed : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String extensionFor(String contentType) {
        return EXTENSIONS.getOrDefault(stripCharset(contentType), "");
    }

    private static String stripCharset(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        return (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim().toLowerCase();
    }

    // A kulcsot mindig a tárolón belülre oldjuk fel; a path-traversal kísérlet hibát ad.
    private Path resolveWithinRoot(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw ApiException.invalidUpload();
        }
        return resolved;
    }

    // --- HMAC-aláírt token: base64url(payload) "." base64url(hmac) ---

    private record SignedTarget(String key, String contentType, long expiresAt) {
    }

    private String sign(String key, String contentType, long expiresAt) {
        String payload = key + "\n" + contentType + "\n" + expiresAt;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        String mac = base64Url(hmac(payloadBytes));
        return base64Url(payloadBytes) + "." + mac;
    }

    private SignedTarget verify(String token) {
        if (token == null) {
            throw ApiException.invalidUpload();
        }
        int dot = token.indexOf('.');
        if (dot < 0) {
            throw ApiException.invalidUpload();
        }
        byte[] payloadBytes;
        byte[] providedMac;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(token.substring(0, dot));
            providedMac = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidUpload();
        }
        if (!MessageDigest.isEqual(providedMac, hmac(payloadBytes))) {
            throw ApiException.invalidUpload();
        }
        String[] parts = new String(payloadBytes, StandardCharsets.UTF_8).split("\n", 3);
        if (parts.length != 3) {
            throw ApiException.invalidUpload();
        }
        try {
            return new SignedTarget(parts[0], parts[1], Long.parseLong(parts[2]));
        } catch (NumberFormatException e) {
            throw ApiException.invalidUpload();
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 nem elérhető", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
