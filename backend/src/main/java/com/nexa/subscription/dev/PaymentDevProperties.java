package com.nexa.subscription.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Fejlesztői/demo kapcsolók a fizetéshez ({@code nexa.payment.dev-controls}). Alapból
 * <b>false</b>; csak akkor true, ha kézzel bekapcsolják (env {@code PAYMENT_DEV_CONTROLS=true}).
 * Bekapcsolva elérhetővé teszi a {@link DevSubscriptionController}-t, amivel élő Paddle nélkül
 * is beállítható az előfizetés-állapot a paywall demózásához/teszteléséhez. Élesben KÖTELEZŐ false.
 */
@Component
@ConfigurationProperties(prefix = "nexa.payment")
public class PaymentDevProperties {

    /** Be vannak-e kapcsolva a fejlesztői előfizetés-vezérlők (dev/demo only). */
    private boolean devControls = false;

    public boolean isDevControls() {
        return devControls;
    }

    public void setDevControls(boolean devControls) {
        this.devControls = devControls;
    }
}
