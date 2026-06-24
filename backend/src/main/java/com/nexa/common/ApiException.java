package com.nexa.common;

import org.springframework.http.HttpStatus;

/**
 * Üzleti hiba egy stabil {@code code}-dal, amit a frontend EN/HU üzenetre fordít.
 * A {@code message} csak fejlesztői/log célokat szolgál.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException emailAlreadyExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                "Email is already registered.");
    }

    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Invalid email or password.");
    }

    public static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "Refresh token is invalid or expired.");
    }

    public static ApiException userNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found.");
    }

    /** Nem létező vagy nem a hívóhoz tartozó bejegyzés (a létezést sem szivárogtatjuk). */
    public static ApiException postNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found.");
    }

    public static ApiException unsupportedImageType() {
        return new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE_TYPE",
                "Only JPEG, PNG, WebP or GIF images are allowed.");
    }

    public static ApiException unsupportedMediaType() {
        return new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_MEDIA_TYPE",
                "Only image (JPEG, PNG, WebP, GIF) or video (MP4, WebM) files are allowed.");
    }

    public static ApiException emptyPost() {
        return new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_POST",
                "A post must have text or at least one media attachment.");
    }

    public static ApiException payloadTooLarge() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "The uploaded file is too large.");
    }

    public static ApiException invalidUpload() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_UPLOAD",
                "The upload link is invalid or has expired.");
    }
}
