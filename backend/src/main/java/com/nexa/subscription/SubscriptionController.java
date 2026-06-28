package com.nexa.subscription;

import com.nexa.subscription.dto.CheckoutInfoDto;
import com.nexa.subscription.dto.PortalDto;
import com.nexa.subscription.dto.SubscriptionDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Az aktuális felhasználó előfizetésének végpontjai. A checkout maga a Paddle
 * hosztolt felületén történik — ide csak az állapot és a kiegészítő adatok jönnek.
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** A bejelentkezett felhasználó előfizetés-állapota (NONE, ha még nincs). */
    @GetMapping("/me")
    public SubscriptionDto me(@AuthenticationPrincipal UUID userId) {
        return subscriptionService.getMine(userId);
    }

    /** A Paddle overlay-checkout indításához szükséges (nem titkos) adatok. */
    @GetMapping("/checkout")
    public CheckoutInfoDto checkout(@AuthenticationPrincipal UUID userId) {
        return subscriptionService.checkoutInfo(userId);
    }

    /** Számlázási portál megnyitható URL-je (kezelés/kártyacsere/lemondás). */
    @PostMapping("/portal")
    public PortalDto portal(@AuthenticationPrincipal UUID userId) {
        return new PortalDto(subscriptionService.portalUrl(userId));
    }
}
