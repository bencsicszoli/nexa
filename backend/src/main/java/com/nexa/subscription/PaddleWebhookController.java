package com.nexa.subscription;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A Paddle webhookok publikus fogadópontja. A végpont nyilvános (a SecurityConfig
 * permitAll-ozza), de az authentikációt a {@code Paddle-Signature} fejléc HMAC-
 * ellenőrzése adja a service-ben — érvénytelen aláírás → 401.
 * A nyers törzset olvassuk be, mert az aláírás pontosan a beérkezett bájtokra szól.
 */
@RestController
@RequestMapping("/api/webhooks/paddle")
public class PaddleWebhookController {

    private final SubscriptionService subscriptionService;

    public PaddleWebhookController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = "Paddle-Signature", required = false) String signature) {
        subscriptionService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
