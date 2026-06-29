package com.nexa.auth.dto;

import java.util.List;

/**
 * A 2FA bekapcsolásakor generált helyreállító kódok (#17) — nyersen, EGYSZER visszaadva.
 * A felhasználónak el kell mentenie őket; a szerver csak a hash-üket tárolja.
 */
public record RecoveryCodesResponse(List<String> recoveryCodes) {
}
