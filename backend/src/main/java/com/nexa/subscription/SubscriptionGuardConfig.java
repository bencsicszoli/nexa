package com.nexa.subscription;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Bekapcsolja az előfizetés-gating interceptort (#15) az {@code /api/**} útvonalakra.
 * Hogy mely végpont gate-elt, azt nem az útvonal, hanem a {@link SubscriptionRequired}
 * annotáció dönti el a kezelőn — így az annotálatlan végpontok (pl. a billing maga) szabadok.
 */
@Configuration
public class SubscriptionGuardConfig implements WebMvcConfigurer {

    private final SubscriptionService subscriptionService;

    public SubscriptionGuardConfig(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SubscriptionGuardInterceptor(subscriptionService))
                .addPathPatterns("/api/**");
    }
}
