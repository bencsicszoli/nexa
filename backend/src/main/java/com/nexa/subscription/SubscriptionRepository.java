package com.nexa.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByPaddleSubscriptionId(String paddleSubscriptionId);

    Optional<Subscription> findByPaddleCustomerId(String paddleCustomerId);
}
