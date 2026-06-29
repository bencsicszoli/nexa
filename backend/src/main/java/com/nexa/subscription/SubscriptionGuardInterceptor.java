package com.nexa.subscription;

import com.nexa.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Előfizetés-gating (#15): ha a kezelő-metódus vagy -osztály {@link SubscriptionRequired},
 * és a hívónak nincs hozzáférése ({@link SubscriptionService#hasAccess}), {@code 402
 * SUBSCRIPTION_REQUIRED}-et dob (a {@code GlobalExceptionHandler} képezi le JSON-ra).
 *
 * <p>A hitelesítés a JWT-szűrő dolga; ez az interceptor utána fut, a principalból olvassa a
 * userId-t. Ha valahogy mégsem hitelesített a kérés, azt nem itt kezeljük (a SecurityConfig 401-et ad).
 */
public class SubscriptionGuardInterceptor implements HandlerInterceptor {

    private final SubscriptionService subscriptionService;

    public SubscriptionGuardInterceptor(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        boolean required = handlerMethod.hasMethodAnnotation(SubscriptionRequired.class)
                || handlerMethod.getBeanType().isAnnotationPresent(SubscriptionRequired.class);
        if (!required) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            // Nem hitelesített — a védettséget a SecurityConfig dönti el (401), nem a paywall.
            return true;
        }
        if (!subscriptionService.hasAccess(userId)) {
            throw ApiException.subscriptionRequired();
        }
        return true;
    }
}
