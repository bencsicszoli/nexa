package com.nexa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Alap CORS-beállítás, hogy a Vite dev szerver (5173) hívhassa az API-t.
 * Később (3. kártya, Spring Security) ezt szigorítjuk a JWT-folyamhoz.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${nexa.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
