package com.nexa.storage;

/**
 * Objektumtároló-absztrakció presigned-URL mintára (lásd CLAUDE.md médiatárolás).
 * A kliens közvetlenül a tárolóba tölt fel, a backend csak az URL-t/kulcsot kezeli.
 *
 * <p>Két implementáció él egymás mellett, a {@code nexa.storage.provider} dönti el:
 * <ul>
 *   <li>{@code local} (alapértelmezett, #4): aláírt PUT a backendre, fájl a lemezre,
 *       kiszolgálás a {@code /api/media} alól — extra infra nélkül azonnal tesztelhető;</li>
 *   <li>{@code s3} (#6): valódi R2/S3 presigned URL. Ugyanezt a szerződést valósítja meg,
 *       így a frontend nem változik az átálláskor.</li>
 * </ul>
 */
public interface StorageService {

    /**
     * Aláírt feltöltési cél létrehozása. A {@code keyPrefix} a logikai mappa
     * (pl. {@code "avatars"}); a tényleges kulcsot a tároló generálja, hogy ne
     * legyen kitalálható és ne ütközzön.
     */
    PresignedUpload createUpload(String keyPrefix, String contentType);

    /** Egy tárolt objektum publikus URL-je (ezt teszi a frontend pl. img src-be). */
    String publicUrl(String key);
}
