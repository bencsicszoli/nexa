package com.nexa.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A Paddle-integráció beállításai ({@code nexa.payment.paddle.*}). A titkok
 * (api-key, webhook-secret) env-változókból jönnek; a client-token publikus,
 * a frontend overlay-checkouthoz kell.
 */
@Component
@ConfigurationProperties(prefix = "nexa.payment.paddle")
public class PaddleProperties {

    /** sandbox | production — a frontend Paddle.js-nek adjuk tovább. */
    private String environment = "sandbox";

    /** A Paddle REST API alap-URL-je (sandbox vagy éles). */
    private String apiBaseUrl = "https://sandbox-api.paddle.com";

    /** Szerver-oldali API kulcs (Bearer) — portál-session, lekérdezések. */
    private String apiKey = "";

    /** Publikus kliens-token a frontend checkouthoz. */
    private String clientToken = "";

    /** A webhook-célpont aláírókulcsa (pdl_ntfset_...) az aláírás-ellenőrzéshez. */
    private String webhookSecret = "";

    /** A havi csomag Paddle price ID-ja (pri_...). */
    private String priceMonthly = "";

    /** Az éves csomag Paddle price ID-ja (pri_...). */
    private String priceAnnual = "";

    /** A próbaidő hossza napokban (a tényleges trial a Paddle price-on van beállítva). */
    private int trialDays = 14;

    /** A webhook-aláírás elfogadott időablaka másodpercben (replay elleni védelem). */
    private long webhookToleranceSeconds = 300;

    public boolean isConfigured() {
        return !apiKey.isBlank() && !clientToken.isBlank()
                && !priceMonthly.isBlank() && !priceAnnual.isBlank();
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getPriceMonthly() {
        return priceMonthly;
    }

    public void setPriceMonthly(String priceMonthly) {
        this.priceMonthly = priceMonthly;
    }

    public String getPriceAnnual() {
        return priceAnnual;
    }

    public void setPriceAnnual(String priceAnnual) {
        this.priceAnnual = priceAnnual;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public long getWebhookToleranceSeconds() {
        return webhookToleranceSeconds;
    }

    public void setWebhookToleranceSeconds(long webhookToleranceSeconds) {
        this.webhookToleranceSeconds = webhookToleranceSeconds;
    }
}
