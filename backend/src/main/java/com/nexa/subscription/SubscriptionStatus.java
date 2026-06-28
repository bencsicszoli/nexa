package com.nexa.subscription;

/**
 * Az előfizetés életciklus-állapota. A Paddle webhookok {@code data.status}
 * mezőjét képezzük le ezekre (lásd {@link SubscriptionService}).
 */
public enum SubscriptionStatus {
    /** Még nem indított checkoutot — nincs Paddle-előfizetés. */
    NONE,
    /** 14 napos próbaidő alatt (kártya megadva, de még nincs terhelés). */
    TRIALING,
    /** Aktív, fizető előfizetés. */
    ACTIVE,
    /** Sikertelen terhelés — a Paddle újrapróbálkozik. */
    PAST_DUE,
    /** Szüneteltetett előfizetés. */
    PAUSED,
    /** Lemondott (a periódus végéig még aktív lehet, de nem újul meg). */
    CANCELED
}
