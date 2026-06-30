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

    /** Nem létező vagy nem a hívóhoz tartozó médiatár-elem (a létezést sem szivárogtatjuk). */
    public static ApiException mediaNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media not found.");
    }

    // --- Ismerősök (#7) ---

    /** Saját magának nem küldhet ismerőskérést. */
    public static ApiException selfFriendRequest() {
        return new ApiException(HttpStatus.BAD_REQUEST, "SELF_FRIEND_REQUEST",
                "You cannot send a friend request to yourself.");
    }

    /** Már ismerősök — nincs mit kezdeményezni. */
    public static ApiException alreadyFriends() {
        return new ApiException(HttpStatus.CONFLICT, "ALREADY_FRIENDS",
                "You are already friends with this user.");
    }

    /** A bejelentkezett felhasználó már küldött (függőben lévő) kérést ennek a felhasználónak. */
    public static ApiException friendRequestAlreadySent() {
        return new ApiException(HttpStatus.CONFLICT, "FRIEND_REQUEST_ALREADY_SENT",
                "You have already sent a friend request to this user.");
    }

    /** A másik fél már küldött kérést — azt kell elfogadni, nem újat küldeni. */
    public static ApiException reverseFriendRequestExists() {
        return new ApiException(HttpStatus.CONFLICT, "REVERSE_FRIEND_REQUEST_EXISTS",
                "This user has already sent you a friend request. Accept it instead.");
    }

    /** Nem létező, vagy nem a hívóhoz tartozó kérés (a létezést sem szivárogtatjuk). */
    public static ApiException friendRequestNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND",
                "Friend request not found.");
    }

    /** A két felhasználó nem ismerős (pl. eltávolításkor). */
    public static ApiException notFriends() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FRIENDS",
                "You are not friends with this user.");
    }

    // --- Követés (#8) ---

    /** Saját magát senki nem követheti. */
    public static ApiException cannotFollowSelf() {
        return new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF",
                "You cannot follow yourself.");
    }

    // --- Csoportok (#9) ---

    /** Nem létező csoport. */
    public static ApiException groupNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found.");
    }

    /** A művelethez (pl. posztolás) tagság kell, de a hívó nem tagja a csoportnak. */
    public static ApiException notGroupMember() {
        return new ApiException(HttpStatus.FORBIDDEN, "NOT_GROUP_MEMBER",
                "You must be a member of this group to do that.");
    }

    /** Az utolsó admin nem léphet ki, amíg más tagok vannak a csoportban. */
    public static ApiException lastAdminCannotLeave() {
        return new ApiException(HttpStatus.CONFLICT, "GROUP_LAST_ADMIN",
                "You are the only admin — promote another member or remove the others before leaving.");
    }

    /** A művelethez csoport-admin jogosultság kell. */
    public static ApiException notGroupAdmin() {
        return new ApiException(HttpStatus.FORBIDDEN, "NOT_GROUP_ADMIN",
                "Only a group admin can do that.");
    }

    /** Nem létező vagy nem ehhez a csoporthoz tartozó csatlakozási kérelem. */
    public static ApiException joinRequestNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "GROUP_JOIN_REQUEST_NOT_FOUND",
                "Join request not found.");
    }

    /** Saját magát az admin nem zárhatja ki (kilépni a /leave végponttal lehet). */
    public static ApiException cannotKickSelf() {
        return new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_KICK_SELF",
                "You cannot remove yourself — use leave instead.");
    }

    /** Admin tagot nem lehet kizárni. */
    public static ApiException cannotKickAdmin() {
        return new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_KICK_ADMIN",
                "You cannot remove a group admin.");
    }

    /** A kizárni kívánt felhasználó nem tagja a csoportnak. */
    public static ApiException targetNotGroupMember() {
        return new ApiException(HttpStatus.NOT_FOUND, "TARGET_NOT_GROUP_MEMBER",
                "That user is not a member of this group.");
    }

    // --- Hozzászólások (#9 kiegészítés) ---

    /** Nem létező hozzászólás/válasz (a létezést sem szivárogtatjuk töröltnél). */
    public static ApiException commentNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found.");
    }

    /** Üres hozzászólás-szöveg nem engedett. */
    public static ApiException emptyComment() {
        return new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_COMMENT",
                "A comment cannot be empty.");
    }

    /** A megadott szülő-hozzászólás nem ehhez a bejegyzéshez tartozik. */
    public static ApiException invalidCommentParent() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COMMENT_PARENT",
                "The parent comment does not belong to this post.");
    }

    // --- Hírfolyam (#10) ---

    /** A lapozási cursor formátuma érvénytelen (sérült vagy nem tőlünk származó érték). */
    public static ApiException invalidCursor() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR",
                "The pagination cursor is invalid.");
    }

    // --- Csevegés (#12) ---

    /** Saját magával nem indíthat kétszemélyes beszélgetést. */
    public static ApiException selfConversation() {
        return new ApiException(HttpStatus.BAD_REQUEST, "SELF_CONVERSATION",
                "You cannot start a conversation with yourself.");
    }

    /** Nem létező, vagy a hívó számára nem elérhető beszélgetés (a létezést sem szivárogtatjuk). */
    public static ApiException conversationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                "Conversation not found.");
    }

    /** Üres üzenet-szöveg nem küldhető. */
    public static ApiException emptyMessage() {
        return new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_MESSAGE",
                "A message cannot be empty.");
    }

    // --- Előfizetés (#14) ---

    /** A Paddle-integráció nincs beállítva (hiányzó kulcs/price ID) — szerveroldali konfig. */
    public static ApiException subscriptionNotConfigured() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SUBSCRIPTION_NOT_CONFIGURED",
                "Subscriptions are not configured on the server.");
    }

    /** A Paddle webhook aláírása érvénytelen, hiányzik vagy lejárt. */
    public static ApiException invalidWebhookSignature() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE",
                "The webhook signature is invalid.");
    }

    /** Nincs Paddle-előfizetés a felhasználóhoz (pl. portál kérése checkout előtt). */
    public static ApiException noSubscription() {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_SUBSCRIPTION",
                "You do not have a subscription yet.");
    }

    // --- Előfizetés-gating (#15) ---

    /**
     * A művelethez aktív előfizetés vagy folyamatban lévő próbaidő kell, de a hívónak
     * nincs (paywall). {@code 402 Payment Required} — a frontend ezt paywallra fordítja.
     */
    public static ApiException subscriptionRequired() {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED, "SUBSCRIPTION_REQUIRED",
                "An active subscription or trial is required.");
    }

    // --- Értesítések / beállítások / 2FA (#17) ---

    /** Nem létező vagy nem a hívóhoz tartozó értesítés. */
    public static ApiException notificationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND",
                "Notification not found.");
    }

    /** Jelszóváltáskor a megadott jelenlegi jelszó nem helyes. */
    public static ApiException wrongPassword() {
        return new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD",
                "The current password is incorrect.");
    }

    /** A megadott 2FA (TOTP vagy recovery) kód érvénytelen. */
    public static ApiException invalid2faCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_2FA_CODE",
                "The verification code is invalid.");
    }

    /** A kétlépcsős login challenge token érvénytelen vagy lejárt. */
    public static ApiException invalidChallengeToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_2FA_CHALLENGE",
                "The two-factor challenge is invalid or has expired.");
    }

    /** A 2FA már be van kapcsolva (újra-bekapcsolás nem értelmezett). */
    public static ApiException twoFactorAlreadyEnabled() {
        return new ApiException(HttpStatus.CONFLICT, "TWO_FACTOR_ALREADY_ENABLED",
                "Two-factor authentication is already enabled.");
    }

    /** 2FA setup/enable-disable lépés, amíg nincs folyamatban lévő beállítás. */
    public static ApiException twoFactorNotInSetup() {
        return new ApiException(HttpStatus.BAD_REQUEST, "TWO_FACTOR_NOT_IN_SETUP",
                "Start the two-factor setup first.");
    }
}
